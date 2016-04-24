import rmi.*;

public class pingserver implements server
{
    public String ping(int idNumber) throws RMIException
    {
        return "Pong" + " " + idNumber;
    }

    public static void main(String args[])
    {
        pingserver ca = new pingserver();
        Skeleton<server> skeleton = new Skeleton(server.class, ca);
        try
        {
            skeleton.start();
        }catch(RMIException e)
        {
            e.printStackTrace();
        }
    }
}