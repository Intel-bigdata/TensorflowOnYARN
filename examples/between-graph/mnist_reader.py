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

import argparse
import os
import sys
import tensorflow as tf
import time
from tensorflow.examples.tutorials.mnist import mnist

FLAGS = None

# Constants used for dealing with the files, matches convert_to_records.
TRAIN_FILE = 'train.tfrecords'
VALIDATION_FILE = 'validation.tfrecords'

def read_and_decode(filename_queue):
  reader = tf.TFRecordReader()
  _, serialized_example = reader.read(filename_queue)
  features = tf.parse_single_example(
      serialized_example,
      # Defaults are not specified since both keys are required.
      features={
          'image_raw': tf.FixedLenFeature([], tf.string),
          'label': tf.FixedLenFeature([], tf.int64),
      })

  # Convert from a scalar string tensor (whose single string has
  # length mnist.IMAGE_PIXELS) to a uint8 tensor with shape
  # [mnist.IMAGE_PIXELS].
  image = tf.decode_raw(features['image_raw'], tf.uint8)
  image.set_shape([mnist.IMAGE_PIXELS])

  # Convert from [0, 255] -> [-0.5, 0.5] floats.
  image = tf.cast(image, tf.float32) * (1. / 255) - 0.5

  # Convert label from a scalar uint8 tensor to an int32 scalar.
  label = tf.cast(features['label'], tf.int32)

  return image, label


def inputs(train, batch_size, num_epochs):
  if not num_epochs: num_epochs = None
  filename = os.path.join(FLAGS.input_data_dir,
                          TRAIN_FILE if train else VALIDATION_FILE)

  with tf.name_scope('input'):
    filename_queue = tf.train.string_input_producer(
        [filename], num_epochs=num_epochs)

    # Even when reading in multiple threads, share the filename
    # queue.
    image, label = read_and_decode(filename_queue)

    # Shuffle the examples and collect them into batch_size batches.
    # (Internally uses a RandomShuffleQueue.)
    # We run this in two threads to avoid being a bottleneck.
    images, sparse_labels = tf.train.shuffle_batch(
        [image, label], batch_size=batch_size, num_threads=2,
        capacity=1000 + 3 * batch_size,
        # Ensures a minimum amount of shuffling of examples.
        min_after_dequeue=1000)

    return images, sparse_labels

def run_training():
  ps_hosts = FLAGS.ps_hosts.split(',')
  worker_hosts = FLAGS.worker_hosts.split(',')
  task_index = FLAGS.task_index
  master = "grpc://" + worker_hosts[task_index]
  logs_path = FLAGS.log_dir + str(task_index)

  # start a server for a specific task
  cluster = tf.train.ClusterSpec({'ps': ps_hosts, 'worker': worker_hosts})

  # Between-graph replication
  with tf.device(tf.train.replica_device_setter(
          worker_device="/job:worker/task:%d" % task_index,
          cluster=cluster)):
    # count the number of updates
    global_step = tf.get_variable('global_step', [],
                                  initializer=tf.constant_initializer(0),
                                  trainable=False)

    images, labels = inputs(train=True, batch_size=FLAGS.batch_size,
                            num_epochs=FLAGS.num_epochs)

    # Build a Graph that computes predictions from the inference model.
    logits = mnist.inference(images,
                             FLAGS.hidden1,
                             FLAGS.hidden2)

    # Add to the Graph the loss calculation.
    loss = mnist.loss(logits, labels)

    # Add to the Graph operations that train the model.
    train_op = mnist.training(loss, FLAGS.learning_rate)

    # The op for initializing the variables.
    init_op = tf.group(tf.global_variables_initializer(),
                       tf.local_variables_initializer())

    # merge all summaries into a single "operation" which we can execute in a session
    summary_op = tf.summary.merge_all()
    print("Variables initialized ...")

    sv = tf.train.Supervisor(is_chief=(task_index == 0),
                             global_step=global_step,
                             init_op=init_op)

    with sv.prepare_or_wait_for_session(master) as sess:

      # create log writer object (this will log on every machine)
      summary_writer = tf.summary.FileWriter(logs_path, graph=tf.get_default_graph())

      coord = sv.coord
      threads = tf.train.start_queue_runners(sess=sess, coord=coord)

      try:
        step = 0
        while not coord.should_stop():
          start_time = time.time()

          _, loss_value, summary = sess.run([train_op, loss, summary_op])

          duration = time.time() - start_time

          # Print an overview fairly often.
          if step % 100 == 0:
            print('Step %d: loss = %.2f (%.3f sec)' % (step, loss_value,
                                                       duration))
            summary_writer.add_summary(summary, step)
            summary_writer.flush()

          step += 1
      except tf.errors.OutOfRangeError:
        print('Done training for %d epochs, %d steps.' % (FLAGS.num_epochs, step))
      finally:
        # When done, ask the threads to stop.
        coord.request_stop()

      # Wait for threads to finish.
      coord.join(threads)
      sess.close()

    sv.stop()
    print("done")

def main(_):
  run_training()


if __name__ == '__main__':
  parser = argparse.ArgumentParser()
  parser.add_argument(
      '--ps_hosts',
      type=str,
      default="",
      help='Ps hosts'
  )
  parser.add_argument(
      '--worker_hosts',
      type=str,
      default="",
      help='Worker hosts'
  )
  parser.add_argument(
      '--task_index',
      type=int,
      default=0,
      help='Task index'
  )
  parser.add_argument(
      '--learning_rate',
      type=float,
      default=0.01,
      help='Initial learning rate.'
  )
  parser.add_argument(
      '--num_epochs',
      type=int,
      default=2,
      help='Number of epochs to run trainer.'
  )
  parser.add_argument(
      '--hidden1',
      type=int,
      default=128,
      help='Number of units in hidden layer 1.'
  )
  parser.add_argument(
      '--hidden2',
      type=int,
      default=32,
      help='Number of units in hidden layer 2.'
  )
  parser.add_argument(
      '--batch_size',
      type=int,
      default=100,
      help='Batch size.'
  )
  parser.add_argument(
      '--input_data_dir',
      type=str,
      default='/tmp/tensorflow/mnist/input_data',
      help='Directory to put the input data.'
  )
  parser.add_argument(
      '--log_dir',
      type=str,
      default='/tmp/tensorflow/mnist/logs/fully_connected_feed',
      help='Directory to put the log data.'
  )

  FLAGS, unparsed = parser.parse_known_args()
  tf.app.run(main=main, argv=[sys.argv[0]] + unparsed)
