import rmi.*;

public interface FactoryInterface
{
    public ServerInterface makePingServer() throws RMIException;
}