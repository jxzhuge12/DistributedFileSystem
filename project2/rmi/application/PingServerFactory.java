package dockerTest;

import rmi.*;
import java.net.*;
import java.io.*;

public class PingServerFactory implements ServerFactory, Serializable {
    private Skeleton<PingServer> skeleton = null;

    public PingServerFactory(PingPongServer server) {
        try{
            this.skeleton = new Skeleton(PingServer.class, server);
            skeleton.start();
        } catch(RMIException e) {
            e.printStackTrace();
        }
    }

    public PingServer makePingServer() throws RMIException, UnknownHostException{
        PingServer serverProxy = null;
        try {
            serverProxy = Stub.create(PingServer.class, skeleton);
            return serverProxy;
        } catch(UnknownHostException e) {
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return serverProxy;
    }
}
