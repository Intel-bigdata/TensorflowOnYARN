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
package org.hdl.tensorflow.yarn.util;

import org.hdl.tensorflow.bridge.TFServer;

public class Constants {

  public static final String DEFAULT_APP_NAME = "TensorFlow";
  public static final String DEFAULT_APP_MASTER_MEMORY = "4096";
  public static final String DEFAULT_APP_MASTER_VCORES = "1";
  public static final String DEFAULT_APP_MASTER_QUEUE = "default";
  public static final String DEFAULT_CONTAINER_MEMORY = "4096";
  public static final String DEFAULT_CONTAINER_VCORES = "1";
  public static final String DEFAULT_TF_WORKER_NUM = "2";
  public static final String DEFAULT_TF_PS_NUM = "1";

  public static final String OPT_TF_APP_NAME = "app_name";
  public static final String OPT_TF_APP_MASTER_MEMORY = "am_memory";
  public static final String OPT_TF_APP_MASTER_VCORES = "am_vcores";
  public static final String OPT_TF_APP_MASTER_QUEUE = "am_queue";
  public static final String OPT_TF_CONTAINER_MEMORY = "container_memory";
  public static final String OPT_TF_CONTAINER_VCORES = "container_vcores";
  public static final String OPT_TF_JAR = "tf_jar";
  public static final String OPT_TF_LIB = "tf_lib";
  public static final String OPT_TF_WORKER_NUM = "num_worker";
  public static final String OPT_TF_PS_NUM = "num_ps";

  public static final String OPT_CLUSTER_SPEC = "cluster_spec";
  public static final String OPT_TASK_INDEX = "task_index";
  public static final String OPT_JOB_NAME = "job_name";

  public static final String TF_JAR_NAME = "tf.jar";
  public static final String TF_LIB_NAME = "lib" + TFServer.TF_LIB + ".so";
}
