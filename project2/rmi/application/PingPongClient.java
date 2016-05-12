package dockerTest;

import rmi.*;
import java.io.*;
import java.net.*;
import java.lang.Integer;

public class PingPongClient {
    /**/
    private static final String hostName = PingPongTestConstants.HOSTNAME;
    private static final int port = PingPongTestConstants.PORT;


    public static void main(String[] args) {
        int idNum = 123456789;
        int TestTime = 4;

        try {
            InetSocketAddress address = new InetSocketAddress(hostName, port);
            ServerFactory factoryProxy = Stub.create(ServerFactory.class, address);
            PingServer serverProxy = factoryProxy.makePingServer(); 
            int successTime = 0;
            Integer number = new Integer(idNum);
            String correctAns = "pong" + number.toString();
            for(int i = 0; i < TestTime; i++) {
                String res = serverProxy.ping(idNum);
                System.out.println("testID: "+ i + "result: " + res);
                if(res.equals(correctAns)) {
                    successTime += 1;
                }
            }
            System.out.println(successTime + "Tests Completed, " + (TestTime-successTime) + "Tests Failed");
        } catch(UnknownHostException e) {
            e.printStackTrace();
        }
        catch(Exception e) {
            e.printStackTrace(); 
        }
    }
}
