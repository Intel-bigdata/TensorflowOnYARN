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

import org.apache.commons.cli.CommandLine;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.hdl.tensorflow.yarn.appmaster.ClusterSpec;
import org.hdl.tensorflow.yarn.util.Constants;
import org.hdl.tensorflow.yarn.util.Utils;

/**
 *  Get {@link ClusterSpec} of an existing TensorFlow Cluster.
 */
public class GetClusterSpec implements Client.Command {

  private final YarnClient yarnClient;
  private final String appId;

  public GetClusterSpec(YarnClient yarnClient, CommandLine cliParser) {
    this.yarnClient = yarnClient;
    if (!cliParser.hasOption(Constants.OPT_APPLICATION_ID)) {
      throw new IllegalArgumentException("Application ID is not specified");
    }
    this.appId = cliParser.getOptionValue(Constants.OPT_APPLICATION_ID);
  }

  @Override
  public boolean run() throws Exception {
    ClusterSpec spec = Client.getClusterSpec(yarnClient, ApplicationId.fromString(appId));
    System.out.println("ClusterSpec: " + Utils.toJsonString(spec.getCluster()));
    return true;
  }

}
