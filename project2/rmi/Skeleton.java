package rmi;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
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
    /** The given class. */
    private Class<T> c;

    /** The server object. */
    private T server;

    /** The address.
     *
     * <p>
     * The address will be null if not given.
     */
    private InetSocketAddress address;

    /** The server socket. */
    private ServerSocket socket;

    /** Skeleton state. 
     *
     * <p>
     * State transfer logic:
     *  During constructing, the state will be initiated as <code>NEW</code>.
     *  NEW: <code>start</code> can be called. <code>start</code> will start the
     *      listening thread, then set the state to <code>STARTED</code>.
     *      <code>stop</code> will throw an RMIException.
     *  STARTED: The listening thread is ready to start but not switched to.
     *      <code>start</code> will throw an RMIException at this time.
     *      <code>stop</code> can be called, which will set the state directly
     *      to <code>INTERRUPTED</code>, thus when the listening thread takes
     *      the CPU, it will directly shut down without accepting any
     *      connnections. If not interrupted, the listening thread will set the
     *      state to <code>RUNNING</code> soon.
     *  RUNNING: The listening thread is accepting connections, and some service
     *      thread may have already been working. <code>start</code> will throw
     *      an RMIException at this time. <code>stop</code> will set the state
     *      to <code>INTERRUPTED</code>, and the listening thread will response
     *      to that soon.
     *  INTERRUPTED: <code>stop</code> has been called, and a shutdown signal
     *      has been sent to the listening thread, but the listening thread has
     *      not been switched to. <code>stop</code> and <code>start</code> will
     *      both throw RMIException at this time.
     *  STOPPING: The listening thread is stopping all service threads and
     *      cleaning up. <code>stop</code> and <code>start</code> will both
     *      throw RMIException at this time.
     *  STOPPED: The listening thread is already stopped. The skeleton is to be
     *      destroyed. <code>stop</code> and <code>start</code> will both throw
     *      RMIExcetion at this time.
     */
    private static enum State {
        NEW, STARTED, RUNNING, INTERRUPTED, STOPPING, STOPPED;
    };

    /** Skeleton state. */
    private State state;

    /** Listening Thread
     * 
     * <p>
     * The listening thread runs the server socket accepting connections,
     * creates <code>ServiceThread</code> for method executing and transfers
     * data with the clients.
     */
    private static class ListeningThread<T> extends Thread
    {
        /** Reference to the outer object. */
        private Skeleton<T> skeleton;

        /** Thread pool. */
        private ExecutorService executor;

        /** Default constructor. 
         *
         * @param skeleton the reference to the skeleton object.
         * @throws NullPointerException if the skeleton is null.
         */
        public ListeningThread(Skeleton<T> skeleton) throws NullPointerException
        {
            super();
            if (skeleton == null) throw new NullPointerException();
            this.skeleton = skeleton;
            this.executor = Executors.newCachedThreadPool();
        }

        /** Main body. */
        public void run() {
            synchronized (skeleton) {
                if (skeleton.state.equals(Skeleton.State.INTERRUPTED)) {
                    skeleton.state = Skeleton.State.STOPPED;
                    skeleton.stopped(null);
                    return;
                } else if (skeleton.state.equals(Skeleton.State.STARTED)) {
                    skeleton.state = Skeleton.State.RUNNING;
                } else {
                    throw new RuntimeException("Skeleton state should be "
                        + "either STARTED or INTERRUPTED when listening "
                        + "thread starts.");
                }
            }
            try {
                while (true) {
                    try {
                        Socket socket = skeleton.socket.accept();
                        this.executor.execute(
                                new ServiceThread<T>(this.skeleton, socket));
                    } catch (SocketException e) {
                        if (skeleton.state.equals(Skeleton.State.INTERRUPTED)) {
                            // normally interrupted
                            throw new InterruptedException();
                        } else if (!skeleton.listen_error(e)) {
                            throw e;
                        }
                    } catch (Exception e) {
                        if (!skeleton.listen_error(e)) {
                            throw e;
                        }
                    }
                }
            } catch (InterruptedException e) {
                // Interrupted, should gracefully shut down all service threads.
                // <reference to line 161>Here we do not need to hold the lock
                // of skeleton, since no other methods can change skeleton.state
                // when the state is INTERRUPTED.
                skeleton.state = Skeleton.State.STOPPING;
                this.executor.shutdown();
                synchronized (skeleton) {
                    skeleton.state = Skeleton.State.STOPPED;
                    skeleton.stopped(null);
                }
            } catch (Exception e) {
                // Still try to gracefully shut down.
                // <reference to line 150>Here we need to hold the lock of
                // skeleton, since <code>stop</code> may change the state.
                synchronized (skeleton) {
                    skeleton.state = Skeleton.State.STOPPING;
                }
                this.executor.shutdown();
                synchronized (skeleton) {
                    skeleton.state = Skeleton.State.STOPPED;
                    skeleton.stopped(e);
                }
            }
        }
    };

    /** Service Thread
     *
     * <p>
     * Each service thread will take charge of a socket connection, runs the
     * required method on the server object, return the value and eventually
     * close the connnection.
     * */
    private static class ServiceThread<T> implements Runnable {
        /** Reference to the outer object. */
        private Skeleton<T> skeleton;

        /** Reference to the socket this thread is dealing with. */
        private Socket socket;

        /** Default constructor. 
         *
         * @param skeleton the reference to the skeleton object.
         * @param socket the connected socket this thread is supposed to be
         *      dealing with.
         * @throws NullPointerException if either skeleton and socket is null.
         */
        public ServiceThread(Skeleton<T> skeleton, Socket socket) 
            throws NullPointerException
        {
            super();
            if (skeleton == null || socket == null) 
                throw new NullPointerException();
            this.skeleton = skeleton;
            this.socket = socket;
        }

        /** Main body. */
        public void run() {
            /** Data-transter protocal: RequestObject and ResponseObject */
            // There are two level of "try-catch" block. The outer try deal with 
            // network. If any exception or error occurs, it's unable to report
            // back to the stub. In the inner try, deall with all method-calling
            // scenarios. InvocationTargetException is METHOD_EXCEPTION.
            try {
                ResponseObject response = null;

                // These part may throw IO exception which may caused by
                // connection error.
                ObjectOutputStream out = new ObjectOutputStream(
                        socket.getOutputStream());
                out.flush();

                ObjectInputStream in = new ObjectInputStream(
                        socket.getInputStream());

                try {
                    /** Get invocation method name and paramters */
                    // readObject may throw IOException or
                    // ClassNotFoundException.
                    RequestObject request = RequestObject.class.cast(in.readObject());
                    Method method = skeleton.c.getMethod(request.method, request.paramTypes);
                    if(request.returnType != method.getReturnType()) {
                        throw new NoSuchMethodException();
                    }

                    /** Invoce method */
                    // The only type that must be explicitly dealt with is the 
                    // InvocationTargetException which means that the method itself, 
                    // not the invoking process throws an exception.
                    Object returnVal = method.invoke(skeleton.server,
                            request.args);
                    response = new
                        ResponseObject(ResponseObject.Type.RETURN_VALUE, 
                                returnVal);
                } catch (InvocationTargetException e) {
                    response = new
                        ResponseObject(ResponseObject.Type.METHOD_EXCEPTION,
                                e.getCause());
                } catch (Exception e) {
                    // NO getCause here, since the exception is the cause!
                    RMIException rmie = new RMIException(e);
                    skeleton.service_error(rmie);
                    response = new
                        ResponseObject(ResponseObject.Type.RMI_EXCEPTION,
                                rmie); 
                }

                out.writeObject(response);
                out.flush();
                socket.close();
            } catch (Exception e) {
                skeleton.service_error(new RMIException(e));
            }            
        }
    };

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
        if(c == null || server == null) {
            throw new NullPointerException();
        }

        if(!c.isInterface()) {
            throw new Error("c is not an interface!");
        }
        if(!RMIExceptionCheck(c)){
            throw new Error("All method in c should throw RMIException!");
        }

        // Assign values to private fields.
        this.c = c;
        this.server = server;
        this.address = null;
        this.state = State.NEW;
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
        if(c == null || server == null) {
            throw new NullPointerException();
        }

        if(!c.isInterface()) {
            throw new Error("c is not an interface!");
        }
        if(!RMIExceptionCheck(c)){
            throw new Error("All method in c should throw RMIException!");
        }
        
        // Assign values to private fields.
        this.c = c;
        this.server = server;
        this.address = address;
        this.state = State.NEW;
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
        // This method is for future overriding. So we do not need to put
        // anything here. However, we need to call this method in a propriate
        // place so that the overriden method will have the right effect.
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
        // As explained in method 'stopped'.
        return false;
    }

    /** Called when an exception occurs at the top level in a service thread.

      <p>
      The default implementation does nothing.

      @param exception The exception that occurred.
      */
    protected void service_error(RMIException exception)
    {
        // As explained in method 'stopped'.
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
        if (!(this.state.equals(State.NEW)
                    || this.state.equals(State.STOPPED))) {
            throw new RMIException("The skeleton is started, "
                + "or not fully stopped.");
        }

        try {
            this.socket = new ServerSocket();
            this.socket.bind(this.address);
            if (this.address == null) {
                this.address = new InetSocketAddress(this.socket.getInetAddress(), 
                        this.socket.getLocalPort());
            }

            ListeningThread<T> thread = new ListeningThread<T>(this);
            thread.start();
            this.state = State.STARTED;
        } catch (IOException e) {
            throw new RMIException("The listening socket cannot be created "
                    + "or bound. Or SO_TIMEOUT cannot be set.", e);
        }
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
        if (!(this.state.equals(State.STARTED)
                || this.state.equals(State.RUNNING))) {
            return;
                }

        this.state = State.INTERRUPTED;
        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Get skeleton class*/
    public Class getInterfaceClass() {
        return this.c;
    }
    
    /** Get skeleton server address*/
    public InetSocketAddress getAddress() {
        return this.address;
    }
    
    /** Check if the state of skeleton is running*/
    public boolean isRunning() {
        return this.state == Skeleton.State.RUNNING || this.state == Skeleton.State.STARTED;
    }

    private static boolean RMIExceptionCheck(Class<?> c) {
        for(Method method : c.getMethods()) {
            boolean hasRMIException = false;
            for(Class<?> exception : method.getExceptionTypes()) {
                if(exception.equals(RMIException.class)) {
                    hasRMIException = true;
                    break;
                }
            }
            if(!hasRMIException) {
                return false;
            }
        }
        return true;
    }
    
    protected static boolean serverCompatibleCheck(Class<?> c, Object server) {
        Class[] theInterfaces = server.getClass().getInterfaces();
        for(Class theInterface : theInterfaces) {
            if(c.equals(theInterface)) {
                return true;
            }
        }
        return false;
    }
}
