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

package com.github.hdl.tensorflow.yarn.rpc.impl;

import com.github.hdl.tensorflow.yarn.rpc.TFApplicationRpc;
import com.github.hdl.tensorflow.yarn.rpc.TensorFlowCluster;
import com.github.hdl.tensorflow.yarn.rpc.GetClusterSpecRequest;
import com.github.hdl.tensorflow.yarn.rpc.GetClusterSpecResponse;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.Server;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.factories.RecordFactory;
import org.apache.hadoop.yarn.factory.providers.RecordFactoryProvider;
import org.apache.hadoop.yarn.ipc.YarnRPC;

import java.io.IOException;
import java.net.InetSocketAddress;

public class TFApplicationRpcServer extends Thread implements TensorFlowCluster {
  private final int rpcPort;
  private final String rpcAddress;
  private final TFApplicationRpc appRpc;


  private static final RecordFactory recordFactory =
      RecordFactoryProvider.getRecordFactory(null);


  public TFApplicationRpcServer(String hostname, TFApplicationRpc rpc) {
    this.rpcAddress = hostname;
    this.rpcPort = 10000 + ((int) (Math.random() * (5000)) + 1);
    this.appRpc = rpc;
  }

  @Override
  public GetClusterSpecResponse getClusterSpec(GetClusterSpecRequest request)
  throws YarnException, IOException {
    GetClusterSpecResponse response = recordFactory.newRecordInstance(GetClusterSpecResponse.class);
    response.setClusterSpec(this.appRpc.getClusterSpec());
    return response;
  }

  public int getRpcPort() {
    return rpcPort;
  }

  @Override
  public void run() {
    Configuration conf = new Configuration();
    YarnRPC rpc = YarnRPC.create(conf);
    InetSocketAddress address = new InetSocketAddress(rpcAddress, rpcPort);
    Server server = rpc.getServer(
        TensorFlowCluster.class, this, address, conf, null,
        conf.getInt(YarnConfiguration.RM_RESOURCE_TRACKER_CLIENT_THREAD_COUNT,
            YarnConfiguration.DEFAULT_RM_RESOURCE_TRACKER_CLIENT_THREAD_COUNT));

    server.start();
  }
}
