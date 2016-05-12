package dockerTest;

import rmi.*;
import java.net.*;

/** a remote factory interface*/
public interface ServerFactory{
    public PingServer makePingServer() throws RMIException, UnknownHostException;
}
