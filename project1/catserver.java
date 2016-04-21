import rmi.*;

public class catserver implements server
{
    public String ping(int idNumber) throws RMIException
    {
        return "Pong" + " " + idNumber;
    }

    public static void main(String args[])
    {
        catserver ca = new catserver();
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