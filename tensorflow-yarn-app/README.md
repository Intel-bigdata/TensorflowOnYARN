TensorFlow on YARN (TOY)
======================
TensorFlow on YARN (TOY) is a toolkit to enable Hadoop users an easy way to run TensorFlow applications in distributed pattern and accomplish tasks including model management and serving inference.

## Goals

 - Support all TensorFlow components on YARN, TensorFlow distributed
   cluster, TensorFlow serving, TensorBoard, etc.
 - Support multi-tenants with consideration of different types of users,
   such as devOp, data scientist and data engineer
 - Support running TensorFlow application in a short-time/long-running
   job manner of both between-graph mode and in-graph mode
 - Support model management to deploy and also support a service layer to handle upper layer's like Spark or web backend inference request easily
 - Minor or no changes required to run user’s existing TensorFlow
   application(can be written in all officially supported languages
   including Python, C++, Java and Go)

Note that current project is a prototype with limitation and is still under development

## Architecture
<p align="center">
<img src=https://cloud.githubusercontent.com/assets/1171680/24279553/7fe214b0-1085-11e7-902e-a331ad61ba23.PNG>
</p>
<p align="center">
Figure1. TOY Architecture
</p>

## Features
- [x] Launch a TensorFlow cluster with specified number of worker and PS server
- [x] Replace python layer with java bridge layer to start server
- [x] Generate ClusterSpec dynamically
- [x] RPC support for client to get ClusterSpec from AM
- [x] Signal handling for graceful shutdown
- [x] Package TensorFlow runtime as a resource that can be distributed easily
- [ ] A launcher script for client mode to run multiple TensorFlow application clients
- [ ] Run in-graph TensorFlow application in client mode
- [ ] TensorBoard support
- [ ] Better handling of network port conflicts
- [ ] Fault tolerance
- [ ] Cluster mode based on Docker
- [ ] Real-time logging support
- [ ] Code refine and more tests

## How-To 
### Set up
1. Git clone ..
2. Compile [tensorflow-bridge](../tensorflow-bridge/README.md) and put "libbridge.so" to "bin" directory that yarn-tf belongs.
3. Compile TensorFlow on YARN

   ```sh
   cd <path_to_hadoop-yarn-application-tensorflow>
   mvn clean package -DskipTests
   ```

### Modify Your Tensorflow Script

1. Since we'll launch TensorFlow servers first and then run your script, the codes that start and join servers is no more needed:
   
    ```
    // the part of your script like the following need to be deleted                       
    server = tf.train.Server(clusterSpec, job_name="worker", task_index=0)      
    server.join()                   
    ```

2. Parse ps and worker servers
   
   Note that at present, you should parse worker and PS server from parameters "ps" and "wk" populated by TensorFlow on YARN client in the form of comma seperated values.
   
3. For a detailed between-graph MNIST example, please refer to:
   [job.py](samples/between-graph/job.py)
   [mnist-client.py](samples/between-graph/mnist-client.py)


### Run  
Run your Tensorflow script. Let's assume a "job.py"

   ```sh
   cd bin
   ydl-tf -job job.py -numberworkers 4 -numberps 1 -jar <path_to_tensorflow-on-yarn-with-dependency_jar>
   ```
