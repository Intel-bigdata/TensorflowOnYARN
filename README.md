## TensorFlowOnYARN [![Build Status](https://travis-ci.org/Intel-bigdata/TensorFlowOnYARN.svg?branch=master)](https://travis-ci.org/Intel-bigdata/TensorFlowOnYARN)

TensorFlow on YARN (TOY) is a toolkit to enable Hadoop users an easy way to run TensorFlow applications in distributed pattern and accomplish tasks including model management and serving inference.
* This project focuses on support of running Tensorflow on YARN, as part of Deep Learning on Hadoop ([HDL](https://github.com/Intel-bigdata/HDL)) effort. 
* [YARN-6043](https://issues.apache.org/jira/browse/YARN-6043)

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
- [x] Run in-graph TensorFlow application in client mode
- [x] TensorBoard support
- [ ] Better handling of network port conflicts
- [ ] Fault tolerance
- [ ] Cluster mode based on Docker
- [ ] Real-time logging support
- [ ] Code refine and more tests

## Quick Start 

0. Prepare the build environment following the instructions from https://www.tensorflow.org/install/install_sources

1. Clone the TensorFlowOnYARN repository
   
   ```bash
   git clone --recursive https://github.com/Intel-bigdata/TensorFlowOnYARN
   ```

2. Build the assembly

   ```bash
   cd TensorFlowOnYARN/tensorflow-parent
   mvn package -Pnative -Pdist
   ```
   
   `tensorflow-yarn-${VERSION}.tar.gz` and `tensorflow-yarn-${VERSION}.zip` are built out 
   in the `tensorflow-parent/tensorflow-yarn-dist/target` directory. Distribute the assembly
   to the client node of a YARN cluster and extract.
   
3. Run the [between-graph mnist example](examples/between-graph/mnist-client.py)

   ```bash
   cd tensorflow-yarn-${VERSION}
   bin/ydl-tf launch --num_worker 2 --num_ps 2
   ```
   
   This will launch a YARN application, which creates a `tf.train.Server` instance for each task.
    A `ClusterSpec` is printed on the console such that you can submit the training script to. e.g.
    
   ```bash
   ClusterSpec: {"ps":["node1:22257","node2:22222"],"worker":["node3:22253","node2:22255"]}
   ```
   
   ```bash
   python examples/between-graph/mnist-client.py \
     --ps_hosts="ps0.hostname:ps0.port,ps1.hostname:ps1.port" \
     --worker_hosts="worker0.hostname:worker0.port,worker1.hostname:worker1.port" \
     --task_index=0
  
   python examples/between-graph/mnist-client.py \
     --ps_hosts="ps0.hostname:ps0.port,ps1.hostname:ps1.port" \
     --worker_hosts="worker0.hostname:worker0.port,worker1.hostname:worker1.port" \
     --task_index=1
   ```

4. To get ClusterSpec of an existing TensorFlow cluster launched by a previous YARN application.

   ```bash
   bin/ydl-tf cluster --app_id <Application ID>
   ```
