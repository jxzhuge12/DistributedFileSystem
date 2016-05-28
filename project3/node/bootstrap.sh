#!/bin/bash

: ${HADOOP_PREFIX:=/usr/local/hadoop}

$HADOOP_PREFIX/etc/hadoop/hadoop-env.sh

rm /tmp/*.pid

# installing libraries if any - (resource urls added comma separated to the ACP system variable)
cd $HADOOP_PREFIX/share/hadoop/common ; for cp in ${ACP//,/ }; do  echo == $cp; curl -LO $cp ; done; cd -

# altering the core-site configuration
# sed s/HOSTNAME/$HOSTNAME/ $HADOOP_PREFIX/etc/hadoop/core-site.xml.template > $HADOOP_PREFIX/etc/hadoop/core-site.xml

if [[ $2 == "slave" ]]; then
  service sshd start
  $HADOOP_PREFIX/sbin/hadoop-daemon.sh start datanode
  $HADOOP_PREFIX/sbin/yarn-daemon.sh start tasktracker
  # $HADOOP_PREFIX/bin/hdfs datanode 2>> /var/log/hadoop/datanode.err >> /var/log/hadoop/datanode.out &
  # nohup $HADOOP_PREFIX/bin/yarn nodemanager 2>> /var/log/hadoop/nodemanager.err >> /var/log/hadoop/nodemanager.out &
fi

if [[ $2 == "master" ]]; then
  service sshd start
  # $HADOOP_PREFIX/bin/hdfs namenode
  yes | $HADOOP_PREFIX/bin/hadoop namenode -format
  $HADOOP_PREFIX/bin/hdfs dfsadmin -safemode leave
  $HADOOP_PREFIX/sbin/yarn-daemon.sh start nodemanager
  $HADOOP_PREFIX/sbin/yarn-daemon.sh start resourcemanager
  # nohup $HADOOP_PREFIX/bin/yarn resourcemanager &
  # nohup $HADOOP_PREFIX/bin/yarn timelineserver &
  # nohup $HADOOP_PREFIX/bin/mapred historyserver &
fi


if [[ $2 == "run" ]]; then
  mkdir input
  echo "Hello Docker dfsa" > input/file2.txt
  echo "Hello Hadoop dfsb" > input/file1.txt
  $HADOOP_PREFIX/bin/hadoop fs -mkdir -p input
  $HADOOP_PREFIX/bin/hadoop fs -put ./input/* input
  echo "start mapreduce"
  $HADOOP_PREFIX/bin/hadoop jar $HADOOP_PREFIX/share/hadoop/mapreduce/hadoop-mapreduce-examples-2.7.0.jar grep input output 'dfs[a-z.]+'
  echo "start print"
  $HADOOP_PREFIX/bin/hadoop fs -cat output/*
fi

# service sshd start


if [[ $1 == "-d" ]]; then
  while true; do sleep 1000; done
fi

if [[ $1 == "-bash" ]]; then
  /bin/bash
fi