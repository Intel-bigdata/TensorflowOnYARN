/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.hdl.tensorflow.yarn.tfserver;

import com.github.hdl.tensorflow.bridge.TFServer;
import com.github.hdl.tensorflow.yarn.appmaster.ClusterSpec;
import com.github.hdl.tensorflow.yarn.util.Constants;
import com.github.hdl.tensorflow.yarn.util.ProcessRunner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;

public class TFServerRunner extends ProcessRunner {
  private static final Log LOG = LogFactory.getLog(TFServerRunner.class);

  private Map<String, List<String>> cluster;
  private String jobName;
  private int taskIndex;

  public static void main(String[] args) {
    TFServerRunner server = new TFServerRunner();
    server.run(args);
  }


  public TFServerRunner() {
    super("TFServerRunner");
  }

  @Override
  public Options initOptions() {
    Options opts = new Options();
    opts.addOption(Constants.OPT_CLUSTER_SPEC, true, "TensorFlow server cluster spec");
    opts.addOption(Constants.OPT_JOB_NAME, true, "TensorFlow server job name");
    opts.addOption(Constants.OPT_TASK_INDEX, true, "TensorFlow server task index");
    return opts;
  }

  @Override
  public void init(CommandLine cliParser) throws Exception {
    cluster = ClusterSpec.fromBase64EncodedJsonString(
        cliParser.getOptionValue(Constants.OPT_CLUSTER_SPEC)).getCluster();
    jobName = cliParser.getOptionValue(Constants.OPT_JOB_NAME);
    taskIndex = Integer.parseInt(cliParser.getOptionValue(Constants.OPT_TASK_INDEX));
  }

  @Override
  public boolean run() {
    String serverName = jobName + ":" + taskIndex;
    LOG.info("Launch a new TensorFlow server " + serverName);

    TFServer server = new TFServer(cluster, jobName, taskIndex);
    server.start();
    server.join();
    LOG.info("TensorFlow server " + serverName + "stopped!");
    return true;
  }
}
