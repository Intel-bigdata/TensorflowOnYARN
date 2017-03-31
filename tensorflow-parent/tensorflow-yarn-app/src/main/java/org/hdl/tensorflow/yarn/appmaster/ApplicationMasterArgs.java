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

import org.hdl.tensorflow.yarn.util.Constants;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ApplicationMasterArgs {

  private static final Log LOG = LogFactory.getLog(ApplicationMasterArgs.class);
  final long containerMemory;
  final int containerVCores;
  final int workerContainerNum;
  final int psContainerNum;
  final int totalContainerNum;
  final String tfLib;
  final String tfJar;

  public ApplicationMasterArgs(CommandLine cliParser, long maxMem, int maxVCores) {
    long mem = Long.parseLong(cliParser.getOptionValue(
        Constants.OPT_TF_CONTAINER_MEMORY, Constants.DEFAULT_CONTAINER_MEMORY));
    int vcores = Integer.parseInt(cliParser.getOptionValue(
        Constants.OPT_TF_CONTAINER_VCORES, Constants.DEFAULT_CONTAINER_VCORES));

    // A resource ask cannot exceed the max.
    if (mem > maxMem) {
      LOG.warn("Container memory specified above max threshold of cluster."
          + " Using max value." + ", specified=" + mem + ", max="
          + maxMem);
      containerMemory = maxMem;
    } else {
      containerMemory = mem;
    }

    if (vcores > maxVCores) {
      LOG.warn("Container vcores specified above max threshold of cluster."
          + " Using max value." + ", specified=" + vcores + ", max="
          + maxMem);
      containerVCores = maxVCores;
    } else {
      containerVCores = vcores;
    }

    workerContainerNum = Integer.parseInt(cliParser.getOptionValue(
        Constants.OPT_TF_WORKER_NUM, Constants.DEFAULT_TF_WORKER_NUM));
    if (workerContainerNum < 1) {
      throw new IllegalArgumentException(
          "Cannot run TensorFlow application with no worker containers");
    }

    psContainerNum = Integer.parseInt(cliParser.getOptionValue(
        Constants.OPT_TF_PS_NUM, Constants.DEFAULT_TF_PS_NUM));
    if (psContainerNum < 0) {
      throw new IllegalArgumentException(
          "Illegal argument of ps containers specified");
    }

    totalContainerNum = workerContainerNum + psContainerNum;

    if (!cliParser.hasOption(Constants.OPT_TF_LIB)) {
      throw new IllegalArgumentException("No TensorFlow JNI library specified");
    }
    tfLib = cliParser.getOptionValue(Constants.OPT_TF_LIB);

    if (!cliParser.hasOption(Constants.OPT_TF_JAR)) {
      throw new IllegalArgumentException("No TensorFlow jar specified");
    }
    tfJar = cliParser.getOptionValue(Constants.OPT_TF_JAR);

  }
}
