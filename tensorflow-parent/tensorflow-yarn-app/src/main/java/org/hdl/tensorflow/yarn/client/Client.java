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

import org.hdl.tensorflow.yarn.appmaster.ClusterSpec;
import org.hdl.tensorflow.yarn.rpc.TFApplicationRpc;
import org.hdl.tensorflow.yarn.rpc.impl.TFApplicationRpcClient;
import org.hdl.tensorflow.yarn.util.Constants;
import org.hdl.tensorflow.yarn.util.ProcessRunner;
import org.hdl.tensorflow.yarn.util.Utils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationReport;
import org.apache.hadoop.yarn.api.records.YarnApplicationState;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;

/**
 * Interact with YARN cluster on client command and options.
 */
public class Client extends ProcessRunner {

  private static final Log LOG = LogFactory.getLog(Client.class);
  private static final String LAUNCH = "launch";
  private static final String CLUSTER = "cluster";
  private final Configuration conf;
  private final YarnClient yarnClient;
  private String cmdName;
  private Command command;

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

  public static void main(String[] args) {
    Client client = new Client();
    client.run(args);
  }


  @Override
  public void init(CommandLine cliParser) {
    if (cmdName.equalsIgnoreCase(LAUNCH)) {
      command = new LaunchCluster(conf, yarnClient, cliParser);
    } else if (cmdName.equalsIgnoreCase(CLUSTER)) {
      command = new GetClusterSpec(yarnClient, cliParser);
    }
  }

  @Override
  public Options initOptions(String[] args) {
    Options options = new Options();
    if (args.length > 0) {
      cmdName = args[0];
      if (cmdName.equalsIgnoreCase(LAUNCH)) {
        Utils.addClientOptions(options);
      } else if (cmdName.equalsIgnoreCase(CLUSTER)) {
        options.addOption(Constants.OPT_APPLICATION_ID, true, "Application ID");
      }
    }

    return options;
  }

  @Override
  public boolean run() throws Exception {
    return command.run();
  }


  interface Command {
    boolean run() throws Exception;
  }

  static ClusterSpec getClusterSpec(YarnClient client, ApplicationId appId) throws Exception {
    ClusterSpec clusterSpec = ClusterSpec.empty();
    ApplicationReport report = client.getApplicationReport(appId);
    YarnApplicationState state = report.getYarnApplicationState();
    if (state.equals(YarnApplicationState.RUNNING)) {
      String hostname = report.getHost();
      int port = report.getRpcPort();
      TFApplicationRpc rpc = TFApplicationRpcClient.getInstance(hostname, port);
      String spec = rpc.getClusterSpec();
      if (spec != null) {
        clusterSpec = ClusterSpec.fromJsonString(spec);
      }
    }
    return clusterSpec;
  }
}
