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
package org.hdl.tensorflow.yarn.appmaster;

import org.hdl.tensorflow.yarn.tfserver.TFServerRunner;
import org.hdl.tensorflow.yarn.tfserver.TFTaskInfo;
import org.hdl.tensorflow.yarn.util.Constants;
import org.hdl.tensorflow.yarn.util.Utils;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;

import java.util.HashMap;
import java.util.Map;

public class LaunchContainerThread extends Thread {

  private static final Log LOG = LogFactory.getLog(LaunchContainerThread.class);

  private final Container container;
  private final String tfLib;
  private final String tfJar;
  private final long containerMemory;
  private final ApplicationMaster appMaster;
  private final TFTaskInfo taskInfo;
  private final ClusterSpec clusterSpec;

  public LaunchContainerThread(Container container, ApplicationMaster appMaster,
      TFTaskInfo taskInfo, ClusterSpec clusterSpec, long containerMemory,
      String tfLib, String tfJar) {
    this.container = container;
    this.appMaster = appMaster;
    this.taskInfo = taskInfo;
    this.clusterSpec = clusterSpec;
    this.containerMemory = containerMemory;
    this.tfLib = tfLib;
    this.tfJar = tfJar;
  }

  @Override
  public void run() {
    try {
      Map<String, String> env = Utils.setJavaEnv(appMaster.getConfiguration());
      String current = ApplicationConstants.Environment.LD_LIBRARY_PATH.$$();
      env.put("LD_LIBRARY_PATH", current + ":" + "`pwd`");

      Map<String, Path> files = new HashMap<>();
      files.put(Constants.TF_JAR_NAME, new Path(tfJar));
      files.put(Constants.TF_LIB_NAME, new Path(tfLib));

      FileSystem fs = FileSystem.get(appMaster.getConfiguration());
      Map<String, LocalResource> localResources =
          Utils.makeLocalResources(fs, files);

      String command = makeContainerCommand(
          containerMemory, clusterSpec.toBase64EncodedJsonString(),
          taskInfo.jobName, taskInfo.taskIndex);

      LOG.info("Launching a new container."
          + ", containerId=" + container.getId()
          + ", containerNode=" + container.getNodeId().getHost()
          + ":" + container.getNodeId().getPort()
          + ", containerNodeURI=" + container.getNodeHttpAddress()
          + ", containerResourceMemory="
          + container.getResource().getMemorySize()
          + ", containerResourceVirtualCores="
          + container.getResource().getVirtualCores()
          + ", command: " + command);
      ContainerLaunchContext ctx = ContainerLaunchContext.newInstance(
          localResources, env, Lists.newArrayList(command), null, null, null, null);
      appMaster.addContainer(container);
      appMaster.getNMClientAsync().startContainerAsync(container, ctx);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String makeContainerCommand(long containerMemory, String clusterSpec,
      String jobName, int taskIndex) {
    String[] commands = new String[]{
        ApplicationConstants.Environment.JAVA_HOME.$$() + "/bin/java",
        "-Xmx" + containerMemory + "m",
        TFServerRunner.class.getName() + " ",
        Utils.mkOption(Constants.OPT_CLUSTER_SPEC, clusterSpec),
        Utils.mkOption(Constants.OPT_JOB_NAME, jobName),
        Utils.mkOption(Constants.OPT_TASK_INDEX, taskIndex),
        "1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/TFServerRunner." +
            ApplicationConstants.STDOUT,
        "2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/TFServerRunner." +
            ApplicationConstants.STDERR
    };

    return Utils.mkString(commands, " ");
  }

}