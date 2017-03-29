/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.hdl.tensorflow.yarn.client;

import com.github.hdl.tensorflow.yarn.appmaster.ApplicationMaster;
import com.github.hdl.tensorflow.yarn.appmaster.ClusterSpec;
import com.github.hdl.tensorflow.yarn.rpc.TFApplicationRpc;
import com.github.hdl.tensorflow.yarn.rpc.impl.TFApplicationRpcClient;
import com.github.hdl.tensorflow.yarn.util.Constants;
import com.github.hdl.tensorflow.yarn.util.ProcessRunner;
import com.github.hdl.tensorflow.yarn.util.Utils;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
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
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import java.util.*;

/**
 * Submits application to YARN and gets {@link ClusterSpec}.
 */
public class Client extends ProcessRunner {

  private static final Log LOG = LogFactory.getLog(Client.class);
  private ClientArgs args;
  private final Configuration conf;
  private final YarnClient yarnClient;

  public Client() {
    this(new YarnConfiguration(), YarnClient.createYarnClient());
  }

  Client(Configuration conf, YarnClient yarnClient) {
    super("Client");
    this.conf = conf;
    this.yarnClient = yarnClient;
    yarnClient.init(conf);
    yarnClient.start();
    LOG.info("Starting YarnClient...");
  }

  public static void main(String[] args) throws ParseException {
    Client client = new Client();
    client.run(args);
  }

  @Override
  public void init(CommandLine cliParser) {
    this.args = new ClientArgs(cliParser);
  }

  @Override
  public Options initOptions() {
    Options options = new Options();
    Utils.addClientOptions(options);
    return options;
  }

  @Override
  public boolean run() throws Exception {
    YarnClientApplication app = createApplication();
    ApplicationId appId = app.getNewApplicationResponse().getApplicationId();

    // Copy the application jar to the filesystem
    FileSystem fs = FileSystem.get(conf);
    String appIdStr = appId.toString();
    Path dstJarPath = Utils.copyLocalFileToDfs(fs, appIdStr, new Path(args.tfJar), Constants.TF_JAR_NAME);
    Path dstLibPath = Utils.copyLocalFileToDfs(fs, appIdStr, new Path(args.tfLib),
        Constants.TF_LIB_NAME);
    Map<String, Path> files = new HashMap<>();
    files.put(Constants.TF_JAR_NAME, dstJarPath);
    Map<String, LocalResource> localResources = Utils.makeLocalResources(fs, files);
    Map<String, String> javaEnv = Utils.setJavaEnv(conf);
    String command = makeAppMasterCommand(args, dstLibPath.toString(), dstJarPath.toString());
    LOG.info("Make ApplicationMaster command: " + command);
    ContainerLaunchContext launchContext = ContainerLaunchContext.newInstance(
        localResources, javaEnv, Lists.newArrayList(command), null, null, null);
    Resource resource = Resource.newInstance(args.amMemory, args.amVCores);
    submitApplication(app, args.appName, launchContext, resource, args.amQueue);
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
    TFApplicationRpc rpc = null;
    while (true) {
      ApplicationReport report = yarnClient.getApplicationReport(appId);
      YarnApplicationState state = report.getYarnApplicationState();
      if (state.equals(YarnApplicationState.RUNNING)) {
        if (null == rpc) {
          String hostname = report.getHost();
          int port = report.getRpcPort();
          rpc = new TFApplicationRpcClient(hostname, port).getRpc();
        }
        String spec = rpc.getClusterSpec();
        if (spec != null) {
          ClusterSpec clusterSpec = ClusterSpec.fromJsonString(spec);
          if (isClusterSpecSatisfied(clusterSpec)) {
            LOG.info("cluster spec is " + Utils.toJsonString(clusterSpec));
            return true;
          }
        }
      } else if (terminated.contains(state)) {
        return false;
      } else {
        Thread.sleep(1000);
      }
    }
  }

  private boolean isClusterSpecSatisfied(ClusterSpec clusterSpec) {
    List<String> worker = clusterSpec.getWorker();
    List<String> ps = clusterSpec.getPs();

    return worker != null && worker.size() == args.workerNum &&
        ps != null && ps.size() == args.psNum;
  }

  private static String makeAppMasterCommand(ClientArgs args, String tfLib, String tfJar) {
    String[] commands = new String[]{
        ApplicationConstants.Environment.JAVA_HOME.$$() + "/bin/java",
        // Set Xmx based on am memory size
        "-Xmx" + args.amMemory + "m",
        // Set class name
        ApplicationMaster.class.getName(),
        Utils.mkOption(Constants.OPT_TF_CONTAINER_MEMORY, args.containerMemory),
        Utils.mkOption(Constants.OPT_TF_CONTAINER_VCORES, args.containerVCores),
        Utils.mkOption(Constants.OPT_TF_WORKER_NUM, args.workerNum),
        Utils.mkOption(Constants.OPT_TF_PS_NUM, args.psNum),
        Utils.mkOption(Constants.OPT_TF_LIB, tfLib),
        Utils.mkOption(Constants.OPT_TF_JAR, tfJar),
        "1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stdout",
        "2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stderr"
    };
    return Utils.mkString(commands, " ");
  }
}
