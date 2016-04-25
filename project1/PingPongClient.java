import java.net.*;
import rmi.*;

public class PingPongClient
{
    public static void main(String args[])
    {
        String hostname = "server";
        int port = 7000;
        InetSocketAddress address = new InetSocketAddress(hostname, port);
        FactoryInterface remote_factory = Stub.create(FactoryInterface.class, address);
        String ping_resut = "";
        try
        {
            ServerInterface remote_server = remote_factory.makePingServer();
            System.out.println(remote_server);
            ping_resut = remote_server.ping(777);
            System.out.println(ping_resut);
            ping_resut = remote_server.ping(777);
            System.out.println(ping_resut);
            ping_resut = remote_server.ping(777);
            System.out.println(ping_resut);
            ping_resut = remote_server.ping(777);
            System.out.println(ping_resut);
        }catch(RMIException e)
        {
            e.printStackTrace();
        }
    }
}
