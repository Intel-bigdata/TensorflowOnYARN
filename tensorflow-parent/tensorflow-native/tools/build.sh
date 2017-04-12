#!/usr/bin/env bash
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License. See accompanying LICENSE file.

cd tensorflow

PATCH=../tools/tensorflow-1.0.0.patch

echo "Applying patch..."
git apply ${PATCH}

if [ "$1" == "1" ];
  then
    echo "Running configure with default options..."
    ./configure << EOF
`which python`



y




EOF
  else
    echo "Running configure..."
    ./configure;
fi

echo "Building TensorFlow..."
bazel build -c opt //tensorflow/core/distributed_runtime/rpc:libbridge.so
git apply --reverse ${PATCH}

cd ..

echo "Done"

