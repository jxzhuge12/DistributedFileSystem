import rmi.*;

public interface ServerInterface
{
    public String ping(int idNumber) throws RMIException;
}