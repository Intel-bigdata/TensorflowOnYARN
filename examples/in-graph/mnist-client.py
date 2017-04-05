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
from __future__ import print_function

import tensorflow as tf
import time
from tensorflow.examples.tutorials.mnist import input_data

# input flags
tf.app.flags.DEFINE_string("ps_hosts", "", "ps hosts")
tf.app.flags.DEFINE_string("worker_hosts", "", "worker hosts")
FLAGS = tf.app.flags.FLAGS

ps_hosts = FLAGS.ps_hosts.split(',')
worker_hosts = FLAGS.worker_hosts.split(',')
master = "grpc://" + worker_hosts[0]

# start a server for a specific task
cluster = tf.train.ClusterSpec({'ps': ps_hosts, 'worker': worker_hosts})

# config
batch_size = 100
learning_rate = 0.0005
training_epochs = 20
logs_path = "/tmp/in-graph/mnist/0"

# load mnist data set
mnist = input_data.read_data_sets('MNIST_data', one_hot=True)

# In-graph replication
with tf.device(tf.train.replica_device_setter(cluster=cluster)):
  # model parameters will change during training so we use tf.Variable
  tf.set_random_seed(1)
  with tf.name_scope("weights"):
    W1 = tf.Variable(tf.random_normal([784, 100]))
    W2 = tf.Variable(tf.random_normal([100, 10]))

  # bias
  with tf.name_scope("biases"):
    b1 = tf.Variable(tf.zeros([100]))
    b2 = tf.Variable(tf.zeros([10]))

  # input images
  with tf.name_scope('input'):
    # None -> batch size can be any size, 784 -> flattened mnist image
    x = tf.placeholder(tf.float32, shape=[None, 784], name="x-input")
    # target 10 output classes
    y_ = tf.placeholder(tf.float32, shape=[None, 10], name="y-input")

  # implement model
  with tf.name_scope("softmax"):
    # y is our prediction
    z2 = tf.add(tf.matmul(x, W1), b1)
    a2 = tf.nn.sigmoid(z2)
    z3 = tf.add(tf.matmul(a2, W2), b2)
    y = tf.nn.softmax(z3)

  # specify cost function
  with tf.name_scope('cross_entropy'):
    # this is our cost
    cross_entropy = tf.reduce_mean(-tf.reduce_sum(y_ * tf.log(y), reduction_indices=[1]))

  train_ops = []
  for i in range(len(worker_hosts)):
    with tf.device("job:worker/task:%d" % i):
      # specify optimizer
      with tf.name_scope('train'):
        # optimizer is an "operation" which we can execute in a session
        grad_op = tf.train.GradientDescentOptimizer(learning_rate)
        train_op = grad_op.minimize(cross_entropy)
        train_ops.append(train_op)

  with tf.name_scope('Accuracy'):
    # accuracy
    correct_prediction = tf.equal(tf.argmax(y, 1), tf.argmax(y_, 1))
    accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32))

  # create a summary for our cost and accuracy
  tf.summary.scalar("cost", cross_entropy)
  tf.summary.scalar("accuracy", accuracy)

  # merge all summaries into a single "operation" which we can execute in a session
  summary_op = tf.summary.merge_all()
  print("Variables initialized ...")


  # count the number of updates
  global_step = tf.get_variable('global_step', [],
                                initializer=tf.constant_initializer(0),
                                trainable=False)
  init_op = tf.global_variables_initializer()
  sv = tf.train.Supervisor(global_step=global_step,
                           init_op=init_op)

  begin_time = time.time()
  frequency = 100
  with sv.prepare_or_wait_for_session(master) as sess:

    # create log writer object (this will log on every machine)
    writer = tf.summary.FileWriter(logs_path, graph=tf.get_default_graph())

    # perform training cycles
    start_time = time.time()
    for epoch in range(training_epochs):

      # number of batches in one epoch
      batch_count = int(mnist.train.num_examples / batch_size)

      count = 0
      for i in range(batch_count):
            batch_x, batch_y = mnist.train.next_batch(batch_size)

            # perform the operations we defined earlier on batch
            _, cost, summary, step = sess.run([train_ops[i % len(train_ops)], cross_entropy, summary_op, global_step],
                                              feed_dict={x: batch_x, y_: batch_y})
            writer.add_summary(summary, step)

            count += 1
            if count % frequency == 0 or i + 1 == batch_count:
              elapsed_time = time.time() - start_time
              start_time = time.time()
              print("Step: %d," % (step + 1),
                    " Epoch: %2d," % (epoch + 1),
                    " Batch: %3d of %3d," % (i + 1, batch_count),
                    " Cost: %.4f," % cost,
                    " AvgTime: %3.2fms" % float(elapsed_time * 1000 / frequency))
              count = 0

    print("Test-Accuracy: %2.2f" % sess.run(accuracy, feed_dict={x: mnist.test.images, y_: mnist.test.labels}))
    print("Total Time: %3.2fs" % float(time.time() - begin_time))
    print("Final Cost: %.4f" % cost)
