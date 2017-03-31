

## How to build Tensroflow native shared library

1. Download Tensorflow's source code to dicrectory ${TENSORFLOW_HOME}.

2. Prepare the build environemnt following the instructions from https://www.tensorflow.org/install/install_sources

3. Adding following code into file ${TENSORFLOW_HOME}/tensorflow/core/distributed_runtime/rpc/BUILD

  ```
   cc_binary(
       name = "libgrpc_tensorflow_server.so",
       linkshared = 1,
       srcs = [
           "grpc_tensorflow_server.cc",
       ],
       deps = [
           ":grpc_server_lib",
           "//tensorflow/core:all_kernels",
           "//tensorflow/core:core_cpu",
           "//tensorflow/core:framework_internal",                                                                                                                 
           "//tensorflow/core:lib",
           "//tensorflow/core:protos_all_cc",
           "//tensorflow/core/distributed_runtime:server_lib",
           "@grpc//:grpc++_unsecure",
       ],
   )
  ```

4. Enter dircetory ${TENSORFLOW_HOME}, then run following command

  ```
  bazel build -c opt //tensorflow/core/distributed_runtime/rpc:libgrpc_tensorflow_server.so
  ```
  
5. Then we will get a shared library `libgrpc_tensorflow_server.so` located at ${TENSORFLOW_HOME}/bazel-bin/tensorflow/core/distributed_runtime/rpc

## How to build Tensorflow's JNI shared library

1. Get the source code of Tensorflow on Yarn project from https://github.com/Intel-bigdata/HDL 

2. Build the library using command:

  ```
  g++ -std=c++11 -shared -o libbridge.so -fPIC -I${JDK_HOME}/include -I${JDK_HOME}/include/linux/ -I${TENSORFLOW_HOME}/bazel-genfiles/ -I${TENSORFLOW_HOME} -L${TENSORFLOW_HOME}/bazel-bin/tensorflow/core/distributed_runtime/rpc -lgrpc_tensorflow_server ${HDL_HOME}/hadoop-deeplearning-project/YARN-TensorFlow/tensorflow-bridge/src/main/native/org_tensorflow_bridge_TFServer.cpp ${HDL_HOME}/hadoop-deeplearning-project/YARN-TensorFlow/tensorflow-bridge/src/main/native/exception_jni.cc
  ```
  
  Please note when building against the latest Tensorflow 1.0, protoc 3.2 may be needed in your build environent. 
   
## How to build java library.
**Please note that hadoop requires protoc 2.5 while tensorflow-bridge needs protoc 3.X which means you need to build this java library using a different environment. However, this java library has already been pulished out and the tenrflow on yarn project depends on that published artifact. So you don't need to compile this project and we'll fix this part in the future.**
 
Here are the main java API it exposed:
 
 ```
 package org.tensorflow.bridge;
 
 public class TFServer {
  public static ServerDef makeServerDef(ServerDef serverDef, String jobName,
    int taskIndex, String proto, ConfigProto config)

  public static ServerDef makeServerDef(ClusterSpec clusterSpec, String jobName,
    int taskIndex, String proto, ConfigProto config)

  public TFServer(ClusterSpec clusterSpec, String jobName, int taskIndex,
                  String proto, ConfigProto config) throws TFServerException

  public TFServer(Map<String,List<String>> clusterSpec, String jobName, int taskIndex)
    throws TFServerException

  public void start()

  public void join()

  public void stop()

  public String getTarget()

  public static TFServer createLocalServer()
}

 ```
