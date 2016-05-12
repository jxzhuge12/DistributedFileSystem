package rmi;

import java.io.Serializable;

// This class must be serializble.
// The java-doc-standard comments looks like this:
/** Data transmission protocol: Response data
 *
 * <p>
 * This class is used for enclosing an RMI response.
 */
public class ResponseObject implements Serializable {
    /** Serialization Version ID. */
    private static final long serialVersionUID = 1L;

    /** Possible response types. */
    public enum Type {
        RETURN_VALUE,
        METHOD_EXCEPTION,
        RMI_EXCEPTION
    };

    /** Response type. */
    public Type type;

    /** Actual response. */
    public Object value;
    
    /** Default constructor. */
    public ResponseObject(Type type, Object value) {
        this.type = type;
        this.value = value;
    }

    public String toString() {
        return "Return type:" + type.toString() + "\n"
            + "Return value:" + value.toString();
    }
}
