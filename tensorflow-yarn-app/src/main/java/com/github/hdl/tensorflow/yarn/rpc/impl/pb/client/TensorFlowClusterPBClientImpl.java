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
package com.github.hdl.tensorflow.yarn.rpc.impl.pb.client;

import com.github.hdl.tensorflow.yarn.rpc.TensorFlowCluster;
import com.github.hdl.tensorflow.yarn.rpc.TensorFlowClusterPB;
import com.github.hdl.tensorflow.yarn.rpc.GetClusterSpecRequest;
import com.github.hdl.tensorflow.yarn.rpc.GetClusterSpecResponse;
import com.github.hdl.tensorflow.yarn.rpc.impl.pb.GetClusterSpecRequestPBImpl;
import com.github.hdl.tensorflow.yarn.rpc.impl.pb.GetClusterSpecResponsePBImpl;
import com.google.protobuf.ServiceException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.ipc.ProtobufRpcEngine;
import org.apache.hadoop.ipc.RPC;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.ipc.RPCUtil;
import org.apache.hadoop.yarn.proto.YarnTensorFlowClusterProtos.GetClusterSpecRequestProto;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

public class TensorFlowClusterPBClientImpl implements TensorFlowCluster, Closeable {
  private TensorFlowClusterPB proxy;

  public TensorFlowClusterPBClientImpl(long clientVersion, InetSocketAddress addr, Configuration conf) throws IOException {
    RPC.setProtocolEngine(conf, TensorFlowClusterPB.class, ProtobufRpcEngine.class);
    proxy = (TensorFlowClusterPB) RPC.getProxy(
      TensorFlowClusterPB.class, clientVersion, addr, conf);
  }

  @Override
  public void close() {
    if(this.proxy != null) {
      RPC.stopProxy(this.proxy);
    }
  }

  @Override
  public GetClusterSpecResponse getClusterSpec(GetClusterSpecRequest request) throws YarnException, IOException {
    GetClusterSpecRequestProto requestProto = ((GetClusterSpecRequestPBImpl)request).getProto();
    try {
      return new GetClusterSpecResponsePBImpl(proxy.getClusterSpec(null, requestProto));
    } catch (ServiceException e) {
      RPCUtil.unwrapAndThrowException(e);
      return null;
    }
  }
}
