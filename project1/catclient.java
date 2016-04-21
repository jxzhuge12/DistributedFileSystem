import java.net.*;
import rmi.*;

public class catclient
{
    public static void main(String args[])
    {
        String hostname = "serverhost";
        int port = 7000;
        InetSocketAddress address = new InetSocketAddress(hostname, port);
        server se = Stub.create(server.class, address);
        String ping_resut = "";
        try
        {
             ping_resut = se.ping(777);
             System.out.println(ping_resut);
        }catch(RMIException e)
        {
            e.printStackTrace();
        }
    }
}