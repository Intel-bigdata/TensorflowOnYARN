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

import com.github.hdl.tensorflow.yarn.util.Constants;
import org.apache.commons.cli.CommandLine;
import org.apache.hadoop.util.ClassUtil;

public class ClientArgs {

  final String appName;
  final Integer amMemory;
  final Integer amVCores;
  final String amQueue;
  final Integer containerMemory;
  final Integer containerVCores;
  final String tfLib;
  final String tfJar;
  final Integer workerNum;
  final Integer psNum;

  ClientArgs(CommandLine cliParser) {
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

    if (!cliParser.hasOption(Constants.OPT_TF_LIB)) {
      throw new IllegalArgumentException("No TensorFlow JNI library specified");
    }

    tfLib = cliParser.getOptionValue(Constants.OPT_TF_LIB);

    if (cliParser.hasOption(Constants.OPT_TF_JAR)) {
      tfJar = cliParser.getOptionValue(Constants.OPT_TF_JAR);
    } else {
      tfJar = ClassUtil.findContainingJar(getClass());
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
}
