package rmi;

import test.*;
import java.net.*;
import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;

public class InvocationRealization implements InvocationHandler, Serializable
{
    public InetSocketAddress address;
    
    public Class c;
    
    public InvocationRealization(InetSocketAddress address, Class c)
    {
        this.address = address;
        this.c = c;
    }
    
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        if(method.getName().equals("equals"))
        {
            if(args.length != 1 || args[0] == null) return false;
            if(args[0] == null) return (Object) false;
            if(!Proxy.isProxyClass(args[0].getClass())) return false;
            InvocationRealization ih = (InvocationRealization)Proxy.getInvocationHandler(args[0]);
            return (Object) (c.equals(ih.c) && address.getAddress().equals(ih.address.getAddress()) && address.getPort() == ih.address.getPort());
        }
        else if(method.getName().equals("hashCode"))
        {
            if(args != null) throw new TestFailed("error");
            return (Object) ((17 + c.hashCode()) * 13 + address.hashCode());
        }
        else if(method.getName().equals("toString"))
        {
            if(args != null) throw new TestFailed("error");
            return (Object) (c.toString() + address.toString());
        }
        else
        {
            Object ret;
            boolean suc;
            
            try
            {
                Socket socket = new Socket(address.getAddress(), address.getPort());
                ObjectOutputStream outStream = new ObjectOutputStream(socket.getOutputStream());
                String methodString = method.getName();
                Class<?>[] argTypes = method.getParameterTypes();
                for (Class<?> argType : argTypes) methodString += argType.getName();
                outStream.writeObject((Object)methodString.hashCode());
                outStream.flush();
                if(args != null)
                {
                    for(Object arg : args)
                    {
                        outStream.writeObject(arg);
                        outStream.flush();
                    }
                }
                ObjectInputStream inStream = new ObjectInputStream(socket.getInputStream());
                suc = (boolean)inStream.readObject();
                ret = inStream.readObject();
                socket.close();
            }
            catch(Throwable e)
            { 
                throw new RMIException("error"); 
            }
            
            if(suc) return ret;
            else throw ((InvocationTargetException)ret).getTargetException();
        }
    } 
}