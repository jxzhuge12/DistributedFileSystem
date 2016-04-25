docker stop $(docker ps -a -q)
docker rm $(docker ps -a -q)
docker build -t myubuntu -f Dockerfile .
docker build -t volume_image -f Dockervolume .
docker create -v /data --name volume_contanier volume_image /bin/true
docker build -t client_image -f Dockerclient .
docker build -t server_image -f Dockerserver .
docker run -d --volumes-from volume_contanier --name server --hostname="serverhost" server_image /bin/bash -c "javac FactoryInterface.java; javac ServerInterface.java; javac PingPongServer.java; javac PingServerFactory.java; java PingServerFactory"
sleep 10s
docker run -d --volumes-from volume_contanier --name client --link server client_image /bin/bash -c "javac ServerInterface.java; javac FactoryInterface.java; javac PingPongClient.java; java PingPongClient"
docker logs -f client
docker logs client > output
python verify.py
rm output
