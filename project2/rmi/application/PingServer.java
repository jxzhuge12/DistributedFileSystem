package dockerTest;

import rmi.*;

/** a remote interface */
public interface PingServer {
    public String ping(int idNumber) throws RMIException;
}
