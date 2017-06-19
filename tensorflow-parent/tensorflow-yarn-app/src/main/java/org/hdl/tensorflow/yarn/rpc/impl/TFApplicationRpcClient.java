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
package org.hdl.tensorflow.yarn.rpc.impl;

import org.hdl.tensorflow.yarn.rpc.GetClusterSpecRequest;
import org.hdl.tensorflow.yarn.rpc.GetClusterSpecResponse;
import org.hdl.tensorflow.yarn.rpc.TFApplicationRpc;
import org.hdl.tensorflow.yarn.rpc.TensorFlowCluster;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.retry.RetryPolicy;
import org.apache.hadoop.io.retry.RetryProxy;
import org.apache.hadoop.yarn.client.RMProxy;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.factories.RecordFactory;
import org.apache.hadoop.yarn.factory.providers.RecordFactoryProvider;

import java.io.IOException;
import java.net.InetSocketAddress;

public class TFApplicationRpcClient implements TFApplicationRpc {
  private RecordFactory recordFactory = RecordFactoryProvider.getRecordFactory(null);
  private TensorFlowCluster tensorflow;
  private static TFApplicationRpcClient instance = null;

  public static TFApplicationRpcClient getInstance(String serverAddress, int serverPort) throws IOException {
    if (null == instance) {
      instance = new TFApplicationRpcClient(serverAddress, serverPort);
    }
    return instance;
  }

  private TFApplicationRpcClient(String serverAddress, int serverPort) throws IOException {
    InetSocketAddress address = new InetSocketAddress(serverAddress, serverPort);
    Configuration conf = new Configuration();
    RetryPolicy retryPolicy = RMProxy.createRetryPolicy(conf);
    TensorFlowCluster proxy = RMProxy.createRMProxy(conf, TensorFlowCluster.class, address);
    this.tensorflow = (TensorFlowCluster) RetryProxy.create(
        TensorFlowCluster.class, proxy, retryPolicy);
  }

  public String getClusterSpec() throws IOException, YarnException {
    GetClusterSpecResponse response =
        this.tensorflow.getClusterSpec(recordFactory.newRecordInstance(GetClusterSpecRequest.class));
    return response.getClusterSpec();
  }
}
