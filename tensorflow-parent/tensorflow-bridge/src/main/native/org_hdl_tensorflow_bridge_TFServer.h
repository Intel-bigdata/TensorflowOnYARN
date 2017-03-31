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
#include <jni.h>
/* Header for class org_hdl_tensorflow_bridge_TFServer */

#ifndef _Included_org_hdl_tensorflow_bridge_TFServer
#define _Included_org_hdl_tensorflow_bridge_TFServer
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     org_hdl_tensorflow_bridge_TFServer
 * Method:    createServer
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL Java_org_hdl_tensorflow_bridge_TFServer_createServer
  (JNIEnv *, jobject, jbyteArray);

/*
 * Class:     org_hdl_tensorflow_bridge_TFServer
 * Method:    startServer
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_hdl_tensorflow_bridge_TFServer_startServer
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_hdl_tensorflow_bridge_TFServer
 * Method:    join
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_hdl_tensorflow_bridge_TFServer_join
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_hdl_tensorflow_bridge_TFServer
 * Method:    stop
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_org_hdl_tensorflow_bridge_TFServer_stop
  (JNIEnv *, jobject, jlong);

/*
 * Class:     org_hdl_tensorflow_bridge_TFServer
 * Method:    target
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_org_hdl_tensorflow_bridge_TFServer_target
  (JNIEnv *, jobject, jlong);

#ifdef __cplusplus
}
#endif
#endif
