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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hdl.tensorflow.yarn.util.Utils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ClusterSpec {

  private static final Log LOG = LogFactory.getLog(ClusterSpec.class);
  private static final String WORKER = "worker";
  private static final String PS = "ps";
  private final Map<String, List<String>> cluster;

  public ClusterSpec() {
    this(Maps.newHashMap());
  }

  private ClusterSpec(Map<String, List<String>> cluster) {
    this.cluster = cluster;
  }

  public static ClusterSpec fromJsonString(String json) throws IOException {
    ObjectMapper objectMapper = new ObjectMapper();
    return new ClusterSpec(objectMapper.readValue(json, Map.class));
  }

  public static ClusterSpec fromBase64EncodedJsonString(
      String base64String) throws IOException {
    String json = decodeBase64(base64String);
    return fromJsonString(json);
  }

  private static String decodeBase64(String base64String) {
    Base64 decoder = new Base64(0, null, true);
    byte[] data = decoder.decode(base64String);
    return new String(data);
  }

  public void addWorkerSpec(String address) {
    if (!cluster.containsKey(WORKER)) {
      cluster.put(WORKER, Lists.newArrayList());
    }
    cluster.get(WORKER).add(address);
  }

  public void addPsSpec(String address) {
    if (!cluster.containsKey(PS)) {
      cluster.put(PS, Lists.newArrayList());
    }
    cluster.get(PS).add(address);
  }

  public Map<String, List<String>> getCluster() {
    return cluster;
  }

  public List<String> getWorker() {
    return cluster.get(WORKER);
  }

  public List<String> getPs() {
    return cluster.get(PS);
  }

  public String toJsonString() throws JsonProcessingException {
    return Utils.toJsonString(cluster);
  }

  public String toBase64EncodedJsonString() throws JsonProcessingException {
    return base64Encoded(toJsonString());
  }

  private String base64Encoded(String json) throws JsonProcessingException {
    byte[] data = json.getBytes();
    Base64 encoder = new Base64(0, null, true);
    return encoder.encodeToString(data);
  }

}