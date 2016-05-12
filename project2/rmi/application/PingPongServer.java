package dockerTest;

import rmi.*;
import java.io.*;
import java.lang.Integer;

/** a remote class*/
public class PingPongServer implements PingServer {

    public String ping(int idNumber) throws RMIException{
        Integer number = new Integer(idNumber);
        return "Pong " + number.toString();
    }

    public static void main(String[] args) {
        try {
            PingServerFactory factory = new PingServerFactory(new PingPongServer());
            Skeleton<ServerFactory> factorySkeleton = new Skeleton(ServerFactory.class, factory);
            factorySkeleton.start();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
