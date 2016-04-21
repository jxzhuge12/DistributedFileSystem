import rmi.*;

public interface server
{
    public String ping(int idNumber) throws RMIException;
}