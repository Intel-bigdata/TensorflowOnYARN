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
package com.github.hdl.tensorflow.yarn.rpc.impl.pb.service;

import com.github.hdl.tensorflow.yarn.rpc.TensorFlowCluster;
import com.github.hdl.tensorflow.yarn.rpc.TensorFlowClusterPB;
import com.github.hdl.tensorflow.yarn.rpc.GetClusterSpecResponse;
import com.github.hdl.tensorflow.yarn.rpc.impl.pb.GetClusterSpecRequestPBImpl;
import com.github.hdl.tensorflow.yarn.rpc.impl.pb.GetClusterSpecResponsePBImpl;
import com.google.protobuf.RpcController;
import com.google.protobuf.ServiceException;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.proto.YarnTensorFlowClusterProtos.GetClusterSpecRequestProto;
import org.apache.hadoop.yarn.proto.YarnTensorFlowClusterProtos.GetClusterSpecResponseProto;

import java.io.IOException;

public class TensorFlowClusterPBServiceImpl implements TensorFlowClusterPB {
  private TensorFlowCluster real;

  public TensorFlowClusterPBServiceImpl(TensorFlowCluster impl) {
    this.real = impl;
  }

  @Override
  public GetClusterSpecResponseProto getClusterSpec(RpcController controller,
      GetClusterSpecRequestProto proto) throws ServiceException {
    GetClusterSpecRequestPBImpl request = new GetClusterSpecRequestPBImpl(proto);
    try {
      GetClusterSpecResponse response = real.getClusterSpec(request);
      return ((GetClusterSpecResponsePBImpl)response).getProto();
    } catch (YarnException | IOException e) {
      throw new ServiceException(e);
    }
  }
}
