/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hdl.tensorflow.yarn.client;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ClassUtil;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.hdl.tensorflow.yarn.appmaster.ApplicationMaster;
import org.hdl.tensorflow.yarn.appmaster.ClusterSpec;
import org.hdl.tensorflow.yarn.util.Constants;
import org.hdl.tensorflow.yarn.util.Utils;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Launch TensorFlow Cluster on YARN and gets {@link ClusterSpec}.
 */
public class LaunchCluster implements Client.Command {
  private static final Log LOG = LogFactory.getLog(LaunchCluster.class);
  private final Configuration conf;
  private final YarnClient yarnClient;
  private final String appName;
  private final Integer amMemory;
  private final Integer amVCores;
  private final String amQueue;
  private final Integer containerMemory;
  private final Integer containerVCores;
  private final String tfLib;
  private final String tfJar;
  private final Integer workerNum;
  private final Integer psNum;

  public LaunchCluster(Configuration conf, YarnClient yarnClient, CommandLine cliParser) {
    this.conf = conf;
    this.yarnClient = yarnClient;
    appName = cliParser.getOptionValue(
        Constants.OPT_TF_APP_NAME, Constants.DEFAULT_APP_NAME);

    amMemory = Integer.parseInt(cliParser.getOptionValue(
        Constants.OPT_TF_APP_MASTER_MEMORY, Constants.DEFAULT_APP_MASTER_MEMORY));
    amVCores = Integer.parseInt(cliParser.getOptionValue(
        Constants.OPT_TF_APP_MASTER_VCORES, Constants.DEFAULT_APP_MASTER_VCORES));
    amQueue = cliParser.getOptionValue(
        Constants.OPT_TF_APP_MASTER_QUEUE, Constants.DEFAULT_APP_MASTER_QUEUE);
    containerMemory = Integer.parseInt(cliParser.getOptionValue(
        Constants.OPT_TF_CONTAINER_MEMORY, Constants.DEFAULT_CONTAINER_MEMORY));
    containerVCores = Integer.parseInt(cliParser.getOptionValue(
        Constants.OPT_TF_CONTAINER_VCORES, Constants.DEFAULT_CONTAINER_VCORES));

    if (cliParser.hasOption(Constants.OPT_TF_JAR)) {
      tfJar = cliParser.getOptionValue(Constants.OPT_TF_JAR);
    } else {
      tfJar = ClassUtil.findContainingJar(getClass());
    }

    if (cliParser.hasOption(Constants.OPT_TF_LIB)) {
      tfLib = cliParser.getOptionValue(Constants.OPT_TF_LIB);
    } else {
      tfLib = Utils.getParentDir(tfJar) + File.separator + Constants.TF_LIB_NAME;
    }

    workerNum = Integer.parseInt(
        cliParser.getOptionValue(Constants.OPT_TF_WORKER_NUM, Constants.DEFAULT_TF_WORKER_NUM));

    if (workerNum <= 0) {
      throw new IllegalArgumentException(
          "Illegal number of TensorFlow worker task specified: " + workerNum);
    }

    psNum = Integer.parseInt(
        cliParser.getOptionValue(Constants.OPT_TF_PS_NUM, Constants.DEFAULT_TF_PS_NUM));

    if (psNum < 0) {
      throw new IllegalArgumentException(
          "Illegal number of TensorFlow ps task specified: " + psNum);
    }
  }

  public boolean run() throws Exception {
    YarnClientApplication app = createApplication();
    ApplicationId appId = app.getNewApplicationResponse().getApplicationId();

    // Copy the application jar to the filesystem
    FileSystem fs = FileSystem.get(conf);
    String appIdStr = appId.toString();
    Path dstJarPath = Utils.copyLocalFileToDfs(fs, appIdStr, new Path(tfJar), Constants.TF_JAR_NAME);
    Path dstLibPath = Utils.copyLocalFileToDfs(fs, appIdStr, new Path(tfLib),
        Constants.TF_LIB_NAME);
    Map<String, Path> files = new HashMap<>();
    files.put(Constants.TF_JAR_NAME, dstJarPath);
    Map<String, LocalResource> localResources = Utils.makeLocalResources(fs, files);
    Map<String, String> javaEnv = Utils.setJavaEnv(conf);
    String command = makeAppMasterCommand(dstLibPath.toString(), dstJarPath.toString());
    LOG.info("Make ApplicationMaster command: " + command);
    ContainerLaunchContext launchContext = ContainerLaunchContext.newInstance(
        localResources, javaEnv, Lists.newArrayList(command), null, null, null);
    Resource resource = Resource.newInstance(amMemory, amVCores);
    submitApplication(app, appName, launchContext, resource, amQueue);
    return awaitApplication(appId);
  }

  YarnClientApplication createApplication() throws Exception {
    return yarnClient.createApplication();
  }

  ApplicationId submitApplication(
      YarnClientApplication app,
      String appName,
      ContainerLaunchContext launchContext,
      Resource resource,
      String queue) throws Exception {
    ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
    appContext.setApplicationName(appName);
    appContext.setApplicationTags(new HashSet<>());
    appContext.setAMContainerSpec(launchContext);
    appContext.setResource(resource);
    appContext.setQueue(queue);

    return yarnClient.submitApplication(appContext);
  }

  boolean awaitApplication(ApplicationId appId) throws Exception {
    Set<YarnApplicationState> terminated = Sets.newHashSet(
        YarnApplicationState.FAILED,
        YarnApplicationState.FINISHED,
        YarnApplicationState.KILLED);
    while (true) {
      ApplicationReport report = yarnClient.getApplicationReport(appId);
      YarnApplicationState state = report.getYarnApplicationState();
      if (state.equals(YarnApplicationState.RUNNING)) {
        ClusterSpec clusterSpec = Client.getClusterSpec(yarnClient, appId);
        if (isClusterSpecSatisfied(clusterSpec)) {
          System.out.println("ClusterSpec: " + Utils.toJsonString(clusterSpec.getCluster()));
          return true;
        }
      } else if (terminated.contains(state)) {
        return false;
      } else {
        Thread.sleep(1000);
      }
    }
  }

  private String makeAppMasterCommand(String tfLib, String tfJar) {
    String[] commands = new String[]{
        ApplicationConstants.Environment.JAVA_HOME.$$() + "/bin/java",
        // Set Xmx based on am memory size
        "-Xmx" + amMemory + "m",
        // Set class name
        ApplicationMaster.class.getName(),
        Utils.mkOption(Constants.OPT_TF_CONTAINER_MEMORY, containerMemory),
        Utils.mkOption(Constants.OPT_TF_CONTAINER_VCORES, containerVCores),
        Utils.mkOption(Constants.OPT_TF_WORKER_NUM, workerNum),
        Utils.mkOption(Constants.OPT_TF_PS_NUM, psNum),
        Utils.mkOption(Constants.OPT_TF_LIB, tfLib),
        Utils.mkOption(Constants.OPT_TF_JAR, tfJar),
        "1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stdout",
        "2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stderr"
    };
    return Utils.mkString(commands, " ");
  }

  private boolean isClusterSpecSatisfied(ClusterSpec clusterSpec) {
    List<String> worker = clusterSpec.getWorker();
    List<String> ps = clusterSpec.getPs();

    return worker != null && worker.size() == workerNum &&
        ps != null && ps.size() == psNum;
  }
}
