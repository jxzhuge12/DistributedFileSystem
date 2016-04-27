import rmi.*;

public interface FactoryInterface
{
    public ServerInterface makePingServer() throws RMIException;
    public void stopPingServer() throws RMIException;
}