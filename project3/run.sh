docker stop master_host slave1_host slave2_host slave3_host slave4_host
docker rm master_host slave1_host slave2_host slave3_host slave4_host

# build image
docker build -t jxzhuge12/node:latest node

echo "start run master node and 4 slave nodes"
docker run -d -p 9000:9000 -p 50070:50070 -p 8088:8088 -p 50010:50010 -p  50020:50020 -p 50030:50030 -h master --name master_host -it jxzhuge12/node:latest /bin/bash
docker run -d -h slave1 --name slave1_host -it jxzhuge12/node:latest /bin/bash
docker run -d -h slave2 --name slave2_host -it jxzhuge12/node:latest /bin/bash
docker run -d -h slave3 --name slave3_host -it jxzhuge12/node:latest /bin/bash
docker run -d -h slave4 --name slave4_host -it jxzhuge12/node:latest /bin/bash

# get IP address of each container
serverIP=$(docker exec master_host ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{print $1}');
slave1IP=$(docker exec slave1_host ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{print $1}');
slave2IP=$(docker exec slave2_host ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{print $1}');
slave3IP=$(docker exec slave3_host ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{print $1}');
slave4IP=$(docker exec slave4_host ifconfig eth0 | grep 'inet addr:' | cut -d: -f2 | awk '{print $1}');

#modify their hosts file
docker exec master_host bash -c "cd /usr/local/hadoop/etc/hadoop/ ; echo -e \"slave1\nslave2\nslave3\nslave4\" > slaves"
docker exec master_host bash -c "cd /etc/ ; echo \"127.0.0.1 localhost\" > hosts; echo -n $serverIP >> hosts; echo \" master\" >> hosts; echo -n $slave1IP >> hosts; echo \" slave1\" >> hosts; echo -n $slave2IP >> hosts; echo \" slave2\" >> hosts; echo -n $slave3IP >> hosts; echo \" slave3\" >> hosts; echo -n $slave4IP >> hosts; echo \" slave4\" >> hosts;"
docker exec master_host bash -c "cd /etc/ ; echo -n \"master\" > hostname;"

docker exec slave1_host bash -c "cd /etc/ ; echo \"127.0.0.1 localhost\" > hosts; echo -n $serverIP >> hosts; echo \" master\" >> hosts; echo -n $slave1IP >> hosts; echo \" slave1\" >> hosts; echo -n $slave2IP >> hosts; echo \" slave2\" >> hosts; echo -n $slave3IP >> hosts; echo \" slave3\" >> hosts; echo -n $slave4IP >> hosts; echo \" slave4\" >> hosts;"
docker exec slave1_host bash -c "cd /etc/ ; echo -n \"slave1\" > hostname;"

docker exec slave2_host bash -c "cd /etc/ ; echo \"127.0.0.1 localhost\" > hosts; echo -n $serverIP >> hosts; echo \" master\" >> hosts; echo -n $slave1IP >> hosts; echo \" slave1\" >> hosts; echo -n $slave2IP >> hosts; echo \" slave2\" >> hosts; echo -n $slave3IP >> hosts; echo \" slave3\" >> hosts; echo -n $slave4IP >> hosts; echo \" slave4\" >> hosts;"
docker exec slave2_host bash -c "cd /etc/ ; echo -n \"slave2\" > hostname;"

docker exec slave3_host bash -c "cd /etc/ ; echo \"127.0.0.1 localhost\" > hosts; echo -n $serverIP >> hosts; echo \" master\" >> hosts; echo -n $slave1IP >> hosts; echo \" slave1\" >> hosts; echo -n $slave2IP >> hosts; echo \" slave2\" >> hosts; echo -n $slave3IP >> hosts; echo \" slave3\" >> hosts; echo -n $slave4IP >> hosts; echo \" slave4\" >> hosts;"
docker exec slave3_host bash -c "cd /etc/ ; echo -n \"slave3\" > hostname;"

docker exec slave4_host bash -c "cd /etc/ ; echo \"127.0.0.1 localhost\" > hosts; echo -n $serverIP >> hosts; echo \" master\" >> hosts; echo -n $slave1IP >> hosts; echo \" slave1\" >> hosts; echo -n $slave2IP >> hosts; echo \" slave2\" >> hosts; echo -n $slave3IP >> hosts; echo \" slave3\" >> hosts; echo -n $slave4IP >> hosts; echo \" slave4\" >> hosts;"
docker exec slave4_host bash -c "cd /etc/ ; echo -n \"slave4\" > hostname;"

# start nodes
docker exec master_host bash -c "/etc/bootstrap.sh -bash master"
docker exec slave1_host bash -c "/etc/bootstrap.sh -bash slave"
docker exec slave2_host bash -c "/etc/bootstrap.sh -bash slave"
docker exec slave3_host bash -c "/etc/bootstrap.sh -bash slave"
docker exec slave4_host bash -c "/etc/bootstrap.sh -bash slave"
docker exec master_host bash -c "/usr/local/hadoop/sbin/start-dfs.sh"
docker exec master_host bash -c "/usr/local/hadoop/sbin/start-yarn.sh"

# report hdfs condition
docker exec master_host bash -c "/usr/local/hadoop/bin/hdfs dfsadmin -report"

# run wordcount
docker exec master_host bash -c "/etc/bootstrap.sh -bash wordcount"

# run bigram
docker exec master_host bash -c "/etc/bootstrap.sh -bash bigram"

docker stop master_host slave1_host slave2_host slave3_host slave4_host
docker rm master_host slave1_host slave2_host slave3_host slave4_host
