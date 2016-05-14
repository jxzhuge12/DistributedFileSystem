package common;

import java.io.*;
import java.util.*;

/** Distributed filesystem paths.

    <p>
    Objects of type <code>Path</code> are used by all filesystem interfaces.
    Path objects are immutable.

    <p>
    The string representation of paths is a forward-slash-delimeted sequence of
    path components. The root directory is represented as a single forward
    slash.

    <p>
    The colon (<code>:</code>) and forward slash (<code>/</code>) characters are
    not permitted within path components. The forward slash is the delimeter,
    and the colon is reserved as a delimeter for application use.
 */
public class Path implements Iterable<String>, Comparable<Path>, Serializable
{
    public ArrayList<String> pathArray;
    
    /** Creates a new path which represents the root directory. */
    public Path()
    {
        pathArray = new ArrayList<String>();
    }

    /** Creates a new path by appending the given component to an existing path.

        @param path The existing path.
        @param component The new component.
        @throws IllegalArgumentException If <code>component</code> includes the
                                         separator, a colon, or
                                         <code>component</code> is the empty
                                         string.
    */
    public Path(Path path, String component)
    {
        if (component == null || component == "" || component.contains(":") || component.contains("/")) 
            throw new IllegalArgumentException("illegal argument");
        pathArray = new ArrayList<String>(path.pathArray);
        pathArray.add(component);
    }

    /** Creates a new path from a path string.

        <p>
        The string is a sequence of components delimited with forward slashes.
        Empty components are dropped. The string must begin with a forward
        slash.

        @param path The path string.
        @throws IllegalArgumentException If the path string does not begin with
                                         a forward slash, or if the path
                                         contains a colon character.
     */
    public Path(String path)
    {
        if (path == null || path == "" || path.contains(":") || path.charAt(0) != '/') 
            throw new IllegalArgumentException("illegal argument");
        pathArray = new ArrayList<String>(Arrays.asList(path.split("/")));
        while(pathArray.remove(""));
    }

    /** Returns an iterator over the components of the path.

        <p>
        The iterator cannot be used to modify the path object - the
        <code>remove</code> method is not supported.

        @return The iterator.
     */
     
    public class MyIterator implements Iterator {
        private Iterator wrappedIterator;
        
        public MyIterator( Iterator it ) {
            wrappedIterator = it;
        }

        public boolean hasNext(){
            return wrappedIterator.hasNext();
        }
        
        public Object next(){
            return wrappedIterator.next();
        }

        public void remove() {
            throw new UnsupportedOperationException("cannot remove");
        }
    }
     
    @Override
    public Iterator<String> iterator()
    {
        return new MyIterator(pathArray.iterator());
    }

    /** Lists the paths of all files in a directory tree on the local
        filesystem.

        @param directory The root directory of the directory tree.
        @return An array of relative paths, one for each file in the directory
                tree.
        @throws FileNotFoundException If the root directory does not exist.
        @throws IllegalArgumentException If <code>directory</code> exists but
                                         does not refer to a directory.
     */
    public static Path[] list(File directory) throws FileNotFoundException
    {
        if(!directory.exists()) throw new FileNotFoundException("cannot find");
        if(!directory.isDirectory()) throw new IllegalArgumentException("is not directory");
        ArrayList<File> files = new ArrayList<File>();
        ArrayList<File> queue = new ArrayList<File>();
        queue.add(directory);
        while(!queue.isEmpty()) {
            if(queue.get(0).isDirectory()) {
                File[] t = queue.get(0).listFiles();
                for(int i = 0; i < t.length; i++) queue.add(t[i]);
            }
            else files.add(queue.get(0));
            queue.remove(0);
        }
        Path[] result = new Path[files.size()];
        for(int i = 0; i < files.size(); i++)
            result[i] = new Path(files.get(i).getPath().substring(directory.getAbsolutePath().length()));
        return result;
    }

    /** Determines whether the path represents the root directory.

        @return <code>true</code> if the path does represent the root directory,
                and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
        return pathArray.isEmpty();
    }

    /** Returns the path to the parent of this path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no parent.
     */
    public Path parent()
    {
        if (isRoot()) throw new IllegalArgumentException("root directory");
        Path parent = new Path(toString());
        parent.pathArray.remove(parent.pathArray.size() - 1);
        return parent;
    }

    /** Returns the last component in the path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no last
                                         component.
     */
    public String last()
    {
        if (isRoot()) throw new IllegalArgumentException("root directory");
        return pathArray.get(pathArray.size() - 1);
    }

    /** Determines if the given path is a subpath of this path.

        <p>
        The other path is a subpath of this path if it is a prefix of this path.
        Note that by this definition, each path is a subpath of itself.

        @param other The path to be tested.
        @return <code>true</code> If and only if the other path is a subpath of
                this path.
     */
    public boolean isSubpath(Path other)
    {
        if(other.pathArray.size() > pathArray.size()) return false;
        for(int i = 0; i < other.pathArray.size(); i++)
            if(!other.pathArray.get(i).equals(pathArray.get(i))) return false;
        return true;
    }

    /** Converts the path to <code>File</code> object.

        @param root The resulting <code>File</code> object is created relative
                    to this directory.
        @return The <code>File</code> object.
     */
    public File toFile(File root)
    {
        if(isRoot()) return root;
        if(root.getPath() == "/") return new File(toString());
        return new File(root.toString() + toString());
    }

    /** Compares this path to another.

        <p>
        An ordering upon <code>Path</code> objects is provided to prevent
        deadlocks between applications that need to lock multiple filesystem
        objects simultaneously. By convention, paths that need to be locked
        simultaneously are locked in increasing order.

        <p>
        Because locking a path requires locking every component along the path,
        the order is not arbitrary. For example, suppose the paths were ordered
        first by length, so that <code>/etc</code> precedes
        <code>/bin/cat</code>, which precedes <code>/etc/dfs/conf.txt</code>.

        <p>
        Now, suppose two users are running two applications, such as two
        instances of <code>cp</code>. One needs to work with <code>/etc</code>
        and <code>/bin/cat</code>, and the other with <code>/bin/cat</code> and
        <code>/etc/dfs/conf.txt</code>.

        <p>
        Then, if both applications follow the convention and lock paths in
        increasing order, the following situation can occur: the first
        application locks <code>/etc</code>. The second application locks
        <code>/bin/cat</code>. The first application tries to lock
        <code>/bin/cat</code> also, but gets blocked because the second
        application holds the lock. Now, the second application tries to lock
        <code>/etc/dfs/conf.txt</code>, and also gets blocked, because it would
        need to acquire the lock for <code>/etc</code> to do so. The two
        applications are now deadlocked.

        @param other The other path.
        @return Zero if the two paths are equal, a negative number if this path
                precedes the other path, or a positive number if this path
                follows the other path.
     */
    @Override
    public int compareTo(Path other)
    {
        if (isSubpath(other)){
            if(other.isSubpath(this)) return 0;
            else return 1;
        }
        else if (other.isSubpath(this)) return -1;
        for (int i = 0; i < Math.min(pathArray.size(), other.pathArray.size()); i++){
            int p = pathArray.get(i).compareTo(other.pathArray.get(i));
            if (p != 0) return p;
        }
        return 0;
    }

    /** Compares two paths for equality.

        <p>
        Two paths are equal if they share all the same components.

        @param other The other path.
        @return <code>true</code> if and only if the two paths are equal.
     */
    @Override
    public boolean equals(Object other)
    {
        return ((Path) other).hashCode() == hashCode();
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
        int hc = 0;
        for(int i = 0; i < pathArray.size(); i++) hc += pathArray.get(i).hashCode();
        return hc;
    }

    /** Converts the path to a string.

        <p>
        The string may later be used as an argument to the
        <code>Path(String)</code> constructor.

        @return The string representation of the path.
     */
    @Override
    public String toString()
    {
        String result = "";
        for(int i = 0; i < pathArray.size(); i++) result += "/" + pathArray.get(i);
        return isRoot() ? "/" : result;
    }
}
