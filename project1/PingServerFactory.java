import java.net.*;
import rmi.*;

public class PingServerFactory implements FactoryInterface
{
    public static void main(String args[])
    {
        InetSocketAddress address = new InetSocketAddress(7000);
        PingServerFactory factory = new PingServerFactory();
        Skeleton<FactoryInterface> skeleton = new Skeleton(FactoryInterface.class, factory, address);
        try
        {
            skeleton.start();
        }catch(RMIException e)
        {
            e.printStackTrace();
        }
    }

    public ServerInterface makePingServer() throws RMIException
    {
        InetSocketAddress address = new InetSocketAddress(8000);
        PingPongServer server = new PingPongServer();
        Skeleton<ServerInterface> skeleton1 = new Skeleton(ServerInterface.class, server, address);
        skeleton1.start();
        ServerInterface remote_server = Stub.create(ServerInterface.class, skeleton1, "serverhost");
        System.out.println(remote_server);
        return remote_server;
    }
}