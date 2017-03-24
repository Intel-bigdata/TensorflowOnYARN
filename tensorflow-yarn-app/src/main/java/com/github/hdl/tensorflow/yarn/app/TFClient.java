/**
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

package com.github.hdl.tensorflow.yarn.app;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class TFClient {

  private static final Log LOG = LogFactory.getLog(TFClient.class);
  public static final String TF_CLIENT_PY = "tf_client.py";
  private String tfClientPy;

  private static final String TF_PY_OPT_WORKERS = "wk";
  private static final String TF_PY_OPT_PSES = "ps";

  private String workers = null;
  private String pses = null;

  private void execCmd(List<String> cmd) {
    try {
      Process process = new ProcessBuilder().command(cmd).inheritIO().start();
      process.waitFor();
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }

  public TFClient(String tfClientPy) {
    this.tfClientPy = tfClientPy;
  }

  public void startTensorBoardClient(String tbEventDir) {
    if (tbEventDir != "") {
      List<String> cmd = new ArrayList<>();
      cmd.add("tensorboard");
      cmd.add("--logdir="+tbEventDir);
      execCmd(cmd);
    }
  }

  public void startTensorflowClient(String clusterSpecJsonString) {
    if (clusterSpecJsonString == null || clusterSpecJsonString.equals("")) {
      return;
    }

    Map<String, List<String>> clusterSpec = null;

    try {
      clusterSpec = ClusterSpec.toClusterMapFromJsonString(clusterSpecJsonString);
    } catch (IOException e) {
      LOG.error("cluster spec is invalid!");
      e.printStackTrace();
      return;
    }

    List<String> workerArray = clusterSpec.get(ClusterSpec.WORKER);
    if (workerArray != null) {
      Iterator<String> it = workerArray.iterator();
      String w = it.next();
      if (w != null) {
        workers = w;
      }

      while (it.hasNext()) {
        workers += "," + it.next();
      }
    }

    List<String> psArray = clusterSpec.get(ClusterSpec.PS);
    if (psArray != null) {
      Iterator<String> it = psArray.iterator();
      String p = it.next();
      if (p != null) {
        pses = p;
      }

      while (it.hasNext()) {
        pses += "," + it.next();
      }
    }

    LOG.info("workers: <" + workers + ">;" + "pses: <" + pses + ">");

    List<String> cmd = new ArrayList<>();
    cmd.add("python");
    cmd.add(tfClientPy);

    if (workers != null) {
      cmd.add("--" + TF_PY_OPT_WORKERS);
      cmd.add(workers);
    }

    if (pses != null) {
      cmd.add("--" + TF_PY_OPT_PSES);
      cmd.add(pses);
    }

    LOG.info("TF client command is [" + cmd + "]");
    execCmd(cmd);
  }
}
