package rmi;

import java.net.*;
import java.io.*;
import java.lang.reflect.*;

/** RMI skeleton

    <p>
    A skeleton encapsulates a multithreaded TCP server. The server's clients are
    intended to be RMI stubs created using the <code>Stub</code> class.

    <p>
    The skeleton class is parametrized by a type variable. This type variable
    should be instantiated with an interface. The skeleton will accept from the
    stub requests for calls to the methods of this interface. It will then
    forward those requests to an object. The object is specified when the
    skeleton is constructed, and must implement the remote interface. Each
    method in the interface should be marked as throwing
    <code>RMIException</code>, in addition to any other exceptions that the user
    desires.

    <p>
    Exceptions may occur at the top level in the listening and service threads.
    The skeleton's response to these exceptions can be customized by deriving
    a class from <code>Skeleton</code> and overriding <code>listen_error</code>
    or <code>service_error</code>.
*/
public class Skeleton<T>
{
    /** Creates a <code>Skeleton</code> with no initial server address. The
        address will be determined by the system when <code>start</code> is
        called. Equivalent to using <code>Skeleton(null)</code>.

        <p>
        This constructor is for skeletons that will not be used for
        bootstrapping RMI - those that therefore do not require a well-known
        port.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server)
    {
        if(c == null || server == null) throw new NullPointerException("parameters cannot be null");
        if(!c.isInterface()) throw new Error("c must be interface!");
        if(!isRemoteInterface(c)) throw new Error("c must be remote interface!");
        this.c = c;
        this.server = server;
        this.address = null;
        //listener = new SkeletonListen();
    }

    /** Creates a <code>Skeleton</code> with the given initial server address.

        <p>
        This constructor should be used when the port number is significant.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @param address The address at which the skeleton is to run. If
                       <code>null</code>, the address will be chosen by the
                       system when <code>start</code> is called.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server, InetSocketAddress address)
    {
        if(c == null || server == null) throw new NullPointerException("parameters cannot be null");
        if(!c.isInterface()) throw new Error("c must be interface!");
        if(!isRemoteInterface(c)) throw new Error("c must be remote interface!");
        this.c = c;
        this.server = server;
        this.address = address;
        //listener = new SkeletonListen();
    }

    private boolean isRemoteInterface(Class<T> c)
    {
        Method[] methods = c.getMethods();
        for(Method method : methods)
        {
            Class<?>[] exceptions = method.getExceptionTypes();
            boolean exist = false;
            for(Class exception : exceptions)
            {
                if (exception.getName().equals("rmi.RMIException")) 
                {
                    exist = true;
                    break;
                }
            }
            if(!exist) return false;
        }
        return true;
    }

    /** Called when the listening thread exits.

        <p>
        The listening thread may exit due to a top-level exception, or due to a
        call to <code>stop</code>.

        <p>
        When this method is called, the calling thread owns the lock on the
        <code>Skeleton</code> object. Care must be taken to avoid deadlocks when
        calling <code>start</code> or <code>stop</code> from different threads
        during this call.

        <p>
        The default implementation does nothing.

        @param cause The exception that stopped the skeleton, or
                     <code>null</code> if the skeleton stopped normally.
     */
    protected void stopped(Throwable cause)
    {
    }

    /** Called when an exception occurs at the top level in the listening
        thread.

        <p>
        The intent of this method is to allow the user to report exceptions in
        the listening thread to another thread, by a mechanism of the user's
        choosing. The user may also ignore the exceptions. The default
        implementation simply stops the server. The user should not use this
        method to stop the skeleton. The exception will again be provided as the
        argument to <code>stopped</code>, which will be called later.

        @param exception The exception that occurred.
        @return <code>true</code> if the server is to resume accepting
                connections, <code>false</code> if the server is to shut down.
     */
    protected boolean listen_error(Exception exception)
    {
        return false;
    }

    /** Called when an exception occurs at the top level in a service thread.

        <p>
        The default implementation does nothing.

        @param exception The exception that occurred.
     */
    protected void service_error(RMIException exception)
    {
    }

    /** Starts the skeleton server.

        <p>
        A thread is created to listen for connection requests, and the method
        returns immediately. Additional threads are created when connections are
        accepted. The network address used for the server is determined by which
        constructor was used to create the <code>Skeleton</code> object.

        @throws RMIException When the listening socket cannot be created or
                             bound, when the listening thread cannot be created,
                             or when the server has already been started and has
                             not since stopped.
     */
    public synchronized void start() throws RMIException
    {
        if(!condition)
        {
            condition = true;
            if(address == null) address = new InetSocketAddress(7000);
            
            try
            {
                serversocket = new ServerSocket(address.getPort());
            }
            catch(IOException e)
            { 
                throw new RMIException("Failed to create serversocket!"); 
            }
            
            try
            {
                listener = new SkeletonListen();
                listener.start();
            }
            catch(Exception e)
            {
                System.out.println(e);
            }
        }
        else throw new RMIException("Listening socket has already created!");
    }

    /** Stops the skeleton server, if it is already running.

        <p>
        The listening thread terminates. Threads created to service connections
        may continue running until their invocations of the <code>service</code>
        method return. The server stops at some later time; the method
        <code>stopped</code> is called at that point. The server may then be
        restarted.
     */
    public synchronized void stop()
    {
        if (condition) 
        {
            condition = false;
            
            try
            {
                Socket lastsocket = new Socket("localhost", address.getPort());
            }
            catch(Throwable e) {  }
            
            try
            {
                serversocket.close();
            }
            catch(IOException e){  }
            stopped(null);
        }
    }
    
    private Class<T> c;
    
    private boolean condition = false;
    
    private T server;
    
    private InetSocketAddress address;
    
    private SkeletonListen listener;
    
    private ServerSocket serversocket;
    
    public int getPort(){
        if(address == null) throw new IllegalStateException();
        return address.getPort();
    }
    
    public InetAddress getAddress(){
        if(address == null) throw new IllegalStateException();
        return address.getAddress();
    }
    
    private class SkeletonListen extends Thread
    {
        public void run()
        {   
            try
            {
                while(condition)
                {
                    Socket socket = serversocket.accept();
                    if(condition)
                    {
                        SkeletonInstance si = new SkeletonInstance(socket);
                        si.start();
                    }
                }
                this.stop();
                stopped(null);
            }
            catch(Exception e){
                listen_error(e);
            }
            catch(Throwable t){
                stopped(t);
            }
        }
    }
    
    private class SkeletonInstance extends Thread implements Serializable
    {
        private Socket socket;
        
        public SkeletonInstance(Socket socket)
        {
            this.socket = socket;
        }
        
        public void run()
        {
            try
            {
                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
                int methodStringhc = (int)inStream.readObject();
                Method[] methods = server.getClass().getMethods();
                for (Method method : methods)
                {
                    String methodString = method.getName();
                    Class<?>[] argTypes = method.getParameterTypes();
                    for (Class<?> argType : argTypes) methodString += argType.getName();
                    if (methodString.hashCode() == methodStringhc)
                    {
                        int l = method.getParameterTypes().length;
                        Object[] args = new Object[l];
                        for (int i = 0; i < l; i++) args[i] = inStream.readObject();
                        ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                        Object ret = null;
                        boolean suc = true;
                        
                        try{
                            method.setAccessible(true);
                            ret = method.invoke(server, args);
                        }
                        catch(InvocationTargetException e)
                        {
                            ret = e;
                            suc = false;
                        }
                        
                        outStream.writeObject((Object)suc);
                        outStream.writeObject(ret);
                        outStream.flush();
                        socket.close();
                        break;
                    }
                }
            }
            catch(Throwable t)
            {
                System.out.println(t);
                service_error(new RMIException("error"));
            }
        }
    }
    
}
