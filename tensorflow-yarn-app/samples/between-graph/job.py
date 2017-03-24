# -*- coding:utf-8 -*-

from __future__ import absolute_import, division, print_function

import getopt
import logging
import os
import subprocess
import sys
import thread
import time

import tensorflow as tf

# input flags
tf.app.flags.DEFINE_string("ps", "", "ps hosts")
tf.app.flags.DEFINE_string("wk", "", "worker hosts")

FLAGS = tf.app.flags.FLAGS

ps_hosts = FLAGS.ps.strip('"').split(',')
worker_hosts = FLAGS.wk.strip('"').split(',')

cluster = tf.train.ClusterSpec({'ps': ps_hosts, 'worker': worker_hosts})


def loop():
    while 1:
        time.sleep(2)
        pass


def cmd(i, target):
    subprocess.call('python mnist-client.py --ps ' + FLAGS.ps + ' --wk ' + FLAGS.wk + ' --job_name="worker"' +
                    ' --task_index=' + str(i), shell=True)


def main(argv):

    for i in range(len(worker_hosts)):
        target = "grpc://" + worker_hosts[i]
        cmd(i, target)

    loop()

if __name__ == '__main__':
    main(sys.argv[1:])
