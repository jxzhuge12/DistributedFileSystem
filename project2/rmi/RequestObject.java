package rmi;

import java.io.Serializable;

/**
 * Data transmission protocal: Request data
 * method: method name to invocate
 * returnType: return type of the method
 * args: arguments of method
 */
// This class must be serializble.
// The java-doc-standard comments looks like this:
/** Data transmission protocol: request data
 *
 * <p>
 * This class is used for enclosing an RMI request.
 */
public class RequestObject implements Serializable {
    /** Serialization Version ID. */
    private static final long serialVersionUID = 1L;

    /** Name of the requested method. */
    public String method;

    /** Class of parameters type*/
    public Class<? extends Object>[] paramTypes;

    /** Class of the return type. */
    public Class<? extends Object> returnType;

    /** Argument list. */
    public Object[] args;

    /** Default constructor.
     *
     * @param method name of the requested method.
     * @param paramTypes types of parameters
     * @param returnType type of the return value.
     * @param args argument list.
     */
    public RequestObject(String methodName, Class<? extends Object>[] paramTypes,
            Class<? extends Object> returnType, Object[] args) {
        this.method = methodName;
        this.paramTypes = paramTypes;
        this.returnType = returnType;
        this.args = args;
    }

    public String toString() {
        String s = "Method:" + method + "\nReturnType:" +
            returnType.toString() + "\nargs:";
        for (Object arg : args) {
            s += arg.getClass().getName() + ":" + arg.toString() + ",";
        }
        return s;
    }
}
