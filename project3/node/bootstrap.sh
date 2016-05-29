#!/bin/bash

: ${HADOOP_PREFIX:=/usr/local/hadoop}

$HADOOP_PREFIX/etc/hadoop/hadoop-env.sh

cd $HADOOP_PREFIX/share/hadoop/common ; for cp in ${ACP//,/ }; do  echo == $cp; curl -LO $cp ; done; cd -

service sshd start

if [[ $2 == "slave" ]]; then
  $HADOOP_PREFIX/sbin/hadoop-daemon.sh start datanode
  $HADOOP_PREFIX/sbin/yarn-daemon.sh start tasktracker
fi

if [[ $2 == "master" ]]; then
  yes | $HADOOP_PREFIX/bin/hadoop namenode -format
  $HADOOP_PREFIX/bin/hdfs dfsadmin -safemode leave
  $HADOOP_PREFIX/sbin/yarn-daemon.sh start nodemanager
  # $HADOOP_PREFIX/sbin/yarn-daemon.sh start resourcemanager
fi

if [[ $2 == "wordcount" ]]; then
  # create file for wordcount
  cd $HADOOP_PREFIX
  mkdir inputdata
  echo "dfsa dfsc dfse dfsa dfsc dfse dfsb dfsd dfsf dfsb dfsd dfsf" > inputdata/file1.txt
  echo "dfsb dfsd dfsf dfsb dfsd dfsf dfsa dfsc dfse dfsa dfsc dfse" > inputdata/file2.txt
  # move file to hdfs
  bin/hadoop fs -mkdir -p input
  bin/hadoop fs -put ./inputdata/* input
  echo "start wordcount"
  bin/hadoop jar share/hadoop/mapreduce/hadoop-mapreduce-examples-2.7.1.jar grep input output 'dfs[a-z.]+'
  echo "print wordcount result"
  bin/hadoop fs -cat output/*
  cd /
fi

if [[ $2 == "bigram" ]]; then
  # set path
  export JAVA_HOME=/usr/java/default
  export PATH=${JAVA_HOME}/bin:${PATH}
  export HADOOP_CLASSPATH=${JAVA_HOME}/lib/tools.jar
  
  cd $HADOOP_PREFIX
  # compile
  bin/hadoop com.sun.tools.javac.Main Bigram.java
  jar cf bg.jar Bigram*.class
  # move file to hdfs
  bin/hadoop fs -mkdir -p bigramInput
  bin/hadoop fs -put ./inputdata/* bigramInput
  echo "start bigram"
  bin/hadoop jar bg.jar Bigram bigramInput bigramOutput
  echo "print bigram result"
  bin/hadoop fs -cat bigramOutput/*
  bin/hadoop fs -cat bigramOutput/* > result
  echo "print homework result"
  python count.py
  cd /
fi

if [[ $1 == "-d" ]]; then
  while true; do sleep 1000; done
fi

if [[ $1 == "-bash" ]]; then
  /bin/bash
fi