import rmi.*;

public class PingPongServer implements ServerInterface
{
    public String ping(int idNumber) throws RMIException
    {
        return "Pong" + " " + idNumber;
    }
}