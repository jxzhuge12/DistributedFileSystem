package rmi;

import java.net.*;
import java.lang.reflect.*;
import java.io.*;

/** RMI stub factory.

  <p>
  RMI stubs hide network communication with the remote server and provide a
  simple object-like interface to their users. This class provides methods for
  creating stub objects dynamically, when given pre-defined interfaces.

  <p>
  The network address of the remote server is set when a stub is created, and
  may not be modified afterwards. Two stubs are equal if they implement the
  same interface and carry the same remote server address - and would
  therefore connect to the same skeleton. Stubs are serializable.
  */
public abstract class Stub
{
    /** ProxyHandler 
     *
     *  <p>
     *  This proxy handler generate a proxy for remote interface, and implement
     *  invocationHandler interface which handle communication between skeleton.
     */
    private static class RMIProxyHandler implements InvocationHandler, Serializable {
        /** interface */
        private Class<?> c;

        /** remote server address*/
        private InetSocketAddress address;

        public RMIProxyHandler(Class<?> c, InetSocketAddress address) {
            this.c = c;
            this.address = address;
        }

        public Object invoke(Object proxy, Method method, Object[] args) 
            throws Exception{
            Socket socket = null;
            ResponseObject response = null;

            // When dispaching method invocations on proxy to delegate object, 3
            // methods from java.lang.Object need sepcial handling: toString(), 
            // hashCode() and equals(Object obj). Because they are related to the
            // proxy object identity, and should be serviced directly by handler.

            /** If equals(Stub stub) is called, return true if the interface
             * class and address is the same.
             * */
            if(method.getName().equals("equals") 
                    && method.getParameterTypes().length == 1 
                    && method.getParameterTypes()[0] == Object.class
                    && method.getReturnType().getName().equals("boolean")) {
                if(args == null || args[0] == null) {
                    return false;
                }

                try {
                    RMIProxyHandler proxyHandler = (RMIProxyHandler) java.lang.reflect.
                        Proxy.getInvocationHandler(proxy);
                    RMIProxyHandler objHandler = (RMIProxyHandler) java.lang.reflect.
                        Proxy.getInvocationHandler(args[0]);

                    if(proxyHandler.getInterface().equals(objHandler.getInterface())
                            && proxyHandler.getAddress().equals(objHandler.getAddress())) {
                        return true;
                    } else {
                        return false;
                    }
                } catch (Exception e) {
                    return false;
                }
            }


            /** If toString() is called, return interfaceName +'@'+ inetAddress 
             * +'@'+ proxyClassName
             * */
            if(method.getName().equals("toString") 
                    && method.getParameterTypes().length == 0 
                    && method.getReturnType().equals(String.class)) {
                RMIProxyHandler proxyHandler = (RMIProxyHandler) java.lang.reflect.
                    Proxy.getInvocationHandler(proxy);
                return proxyHandler.getInterface().getName() + "@" + proxyHandler.
                    getAddress().toString() + "@" + proxy.getClass().getName();
                    }


            /** If hashCode() is called. retirm proxyHashCode + '@' + handlerHashCode
            */
            if(method.getName().equals("hashCode") 
                    && method.getParameterTypes().length == 0 
                    && method.getReturnType().getName().equals("int")) {
                RMIProxyHandler proxyHandler = (RMIProxyHandler) java.lang.reflect.
                    Proxy.getInvocationHandler(proxy);
                return proxyHandler.getInterface().hashCode() + 
                    proxyHandler.getAddress().hashCode();
            }

            try {
                /* Establish socket connection and transfer request object */
                socket = new Socket(this.address.getHostName(), 
                        this.address.getPort());
                ObjectOutputStream out = 
                    new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                ObjectInputStream in = 
                    new ObjectInputStream(socket.getInputStream());
                RequestObject request = new RequestObject(method.getName(), 
                        method.getParameterTypes(),
                        method.getReturnType(), args);
                out.writeObject(request);
                response = ResponseObject.class.cast(in.readObject());
                socket.close();
            } catch(Exception e) {
                throw new RMIException(e); 
            }

            if(response != null) {
                if(response.type == ResponseObject.Type.METHOD_EXCEPTION) {
                    throw Exception.class.cast(response.value);
                } else if(response.type == ResponseObject.Type.RMI_EXCEPTION) {
                    throw RMIException.class.cast(response.value); 
                } else {
                    return response.value;
                }
            } else {
                throw new RMIException("No response received.");
            }
        }

        /** Getters */
        public Class<?> getInterface() {
            return this.c;
        }

        public InetSocketAddress getAddress() {
            return this.address;
        }
    }

    /** Creates a stub, given a skeleton with an assigned adress.

      <p>
      The stub is assigned the address of the skeleton. The skeleton must
      either have been created with a fixed address, or else it must have
      already been started.

      <p>
      This method should be used when the stub is created together with the
      skeleton. The stub may then be transmitted over the network to enable
      communication with the skeleton.

      @param c A <code>Class</code> object representing the interface
      implemented by the remote object.
      @param skeleton The skeleton whose network address is to be used.
      @return The stub created.
      @throws IllegalStateException If the skeleton has not been assigned an
      address by the user and has not yet been
      started.
      @throws UnknownHostException When the skeleton address is a wildcard and
      a port is assigned, but no address can be
      found for the local host.
      @throws NullPointerException If any argument is <code>null</code>.
      @throws Error If <code>c</code> does not represent a remote interface
      - an interface in which each method is marked as throwing
      <code>RMIException</code>, or if an object implementing
      this interface cannot be dynamically created.
      */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton)
        throws UnknownHostException
    {
        // 1. check null pointer
        // 2. check state of skeleton
        // 3. check interface compatibility
        // 4. check "UnknownHostException" /XXX
        if(c == null || skeleton == null) {
            throw new NullPointerException();
        }

        if(skeleton.getAddress() == null ||
                skeleton.getAddress().getHostName() == null) {
            throw new IllegalStateException();
                }

        if(!c.isInterface()) {
            throw new Error("c is not an interface!");
        }
        if(!RMIExceptionCheck(c)) {
            throw new Error("All methods in c should throw RMIException");
        }

        RMIProxyHandler proxyHandler = new RMIProxyHandler(c, 
                skeleton.getAddress());
        Object proxy = (T)java.lang.reflect.Proxy.newProxyInstance(
                c.getClassLoader(), new Class[]{c}, proxyHandler); 
        return  (T)proxy;

    }

    /** Creates a stub, given a skeleton with an assigned address and a hostname
      which overrides the skeleton's hostname.

      <p>
      The stub is assigned the port of the skeleton and the given hostname.
      The skeleton must either have been started with a fixed port, or else
      it must have been started to receive a system-assigned port, for this
      method to succeed.

      <p>
      This method should be used when the stub is created together with the
      skeleton, but firewalls or private networks prevent the system from
      automatically assigning a valid externally-routable address to the
      skeleton. In this case, the creator of the stub has the option of
      obtaining an externally-routable address by other means, and specifying
      this hostname to this method.

      @param c A <code>Class</code> object representing the interface
      implemented by the remote object.
      @param skeleton The skeleton whose port is to be used.
      @param hostname The hostname with which the stub will be created.
      @return The stub created.
      @throws IllegalStateException If the skeleton has not been assigned a
      port.
      @throws NullPointerException If any argument is <code>null</code>.
      @throws Error If <code>c</code> does not represent a remote interface
      - an interface in which each method is marked as throwing
      <code>RMIException</code>, or if an object implementing
      this interface cannot be dynamically created.
      */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton,
            String hostname)
    {
        if(c == null || skeleton == null || hostname == null) {
            throw new NullPointerException();
        }

        if(skeleton.getAddress() == null ||
                skeleton.getAddress().getHostName() == null) {
            throw new IllegalStateException();
                }

        if(!c.isInterface()) {
            throw new Error("c is not an interface!");
        }
        if(!RMIExceptionCheck(c)) {
            throw new Error("All methods in c should throw RMIException");
        }

        RMIProxyHandler proxyHandler = new RMIProxyHandler(c, new 
                InetSocketAddress(hostname, skeleton.getAddress().getPort()));
        Object proxy = (T)java.lang.reflect.Proxy.newProxyInstance(
                c.getClassLoader(), new Class[]{c}, proxyHandler); 
        return  (T)proxy;
        }

    /** Creates a stub, given the address of a remote server.

      <p>
      This method should be used primarily when bootstrapping RMI. In this
      case, the server is already running on a remote host but there is
      not necessarily a direct way to obtain an associated stub.

      @param c A <code>Class</code> object representing the interface
      implemented by the remote object.
      @param address The network address of the remote skeleton.
      @return The stub created.
      @throws NullPointerException If any argument is <code>null</code>.
      @throws Error If <code>c</code> does not represent a remote interface
      - an interface in which each method is marked as throwing
      <code>RMIException</code>, or if an object implementing
      this interface cannot be dynamically created.
      */
    public static <T> T create(Class<T> c, InetSocketAddress address)
    {
        if(c == null || address == null) {
            throw new NullPointerException();
        }

        if(!c.isInterface()) {
            throw new Error("c is not an interface!");
        }
        if(!RMIExceptionCheck(c)) {
            throw new Error("All methods in c should throw RMIException");
        }

        RMIProxyHandler proxyHandler = new RMIProxyHandler(c, address);
        Object proxy = (T)java.lang.reflect.Proxy.newProxyInstance(
                c.getClassLoader(), new Class[]{c}, proxyHandler); 
        return  (T)proxy;
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
}
