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
    private static final long serialVersionUID = 6641292594239992029L;
    ArrayList<String> components;
    
    /** Creates a new path which represents the root directory. */
    public Path()
    {
        components = new ArrayList<String>();
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
	if(component.contains("/") || component.contains(":")  
		|| component.length() == 0){
	    throw new IllegalArgumentException("Error. The component includes " +
	    		"the separator,a colon or is empty string.");
	}
	components = new ArrayList<String>();
	components.addAll(path.components);
	components.add(component);    
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
	components = new ArrayList<String>();
        if (!path.startsWith("/") || path.contains(":") ){
            throw new IllegalArgumentException("Error. The path string does not" +
            		"begin with a forward slash, or it contains a colon character.");
        }
        
        String[] strings = path.split("/");
        for(String string : strings){
            //Drop the empty components
            if(!string.isEmpty())
            components.add(string);
        }
    }

    /** Returns an iterator over the components of the path.

        <p>
        The iterator cannot be used to modify the path object - the
        <code>remove</code> method is not supported.

        @return The iterator.
     */
    @Override
    public Iterator<String> iterator()
    {
	// mainly use the class to disable the remove method
        class PathIterator implements Iterator<String>{
            Iterator<String> iter;
            
            public PathIterator(Iterator<String> iter){
        	this.iter = iter;
            }
            
	    @Override
	    public boolean hasNext() {		
		return iter.hasNext();
	    }

	    @Override
	    public String next() {	
		return iter.next();
	    }

	    @Override
	    public void remove() {
		throw new UnsupportedOperationException("Remove is not supported");	
	    }
            
        }
        PathIterator pathIterator = new PathIterator(components.iterator());
        return pathIterator;
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
        if(!directory.exists()){
            throw new FileNotFoundException("The root directory does not exist");
        }
        if(!directory.isDirectory()){
            throw new IllegalArgumentException("The directory exists but does not " +
            		"refer to a directory.");
        }
        
        File[] files = directory.listFiles();
        ArrayList<Path> paths = new ArrayList<Path>();
        for(File file:files){
            // If it is a directory, then do recursively
            if(file.isDirectory()){
        	Path[] childrenPaths = list(file);
        	for(Path childrenPath:childrenPaths){
        	    Path tempPath = new Path(new Path(),file.getName());
        	   tempPath.components.addAll(childrenPath.components);
        	    paths.add(tempPath);
        	}
            }
            else if(file.isFile()){
        	paths.add(new Path(new Path(),file.getName()));
            }
        }
        return (Path[]) paths.toArray(new Path[0]);
    }
   
    /** Determines whether the path represents the root directory.

        @return <code>true</code> if the path does represent the root directory,
                and <code>false</code> if it does not.
     */
    public boolean isRoot()
    {
            return components.isEmpty();
    }

    /** Returns the path to the parent of this path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no parent.
     */
    public Path parent()
    {
        if(isRoot()){
            throw new IllegalArgumentException("Error. The path is root.");
        }
        Path parentPath = new Path();
        parentPath.components.addAll(components);
        parentPath.components.remove(parentPath.components.size()-1);
        return parentPath;
    }

    /** Returns the last component in the path.

        @throws IllegalArgumentException If the path represents the root
                                         directory, and therefore has no last
                                         component.
     */
    public String last()
    {
	 if(isRoot()){
	            throw new IllegalArgumentException("Error. The path is root.");
	 }
	 return components.get(components.size()-1);
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
        ArrayList<String> otherComponents = other.components;
        // If it's larger than this path, then of course not a subpath
        if(otherComponents.size()>components.size()){
            return false;
        }
        for(int i=0;i<otherComponents.size();i++){
            if(!components.get(i).equals(otherComponents.get(i))){
        	return false;
            }
        }
        return true;
    }

    /** Converts the path to <code>File</code> object.

        @param root The resulting <code>File</code> object is created relative
                    to this directory.
        @return The <code>File</code> object.
     */
    public File toFile(File root)
    {
	return new File(root.getPath().concat(this.toString()));
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
        int minSize = Math.min(this.components.size(),other.components.size());
        for(int i=0;i<minSize;i++){
            int tempResult = this.components.get(i).compareTo(other.components.get(i));
            if(tempResult < 0){
        	return -1;
            }
            else if(tempResult > 0){
        	return 1;
            }
        }
        if(this.components.size() > other.components.size()){
            return 1;
        }
        else if(this.components.size() < other.components.size()){
            return -1;
        }
        else{
            return 0;
        }
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
	ArrayList<String> otherComponents = ((Path)other).components;
        // If it's larger than this path, then of course not a subpath
        if(otherComponents.size()!=components.size()){
            return false;
        }
        for(int i=0;i<otherComponents.size();i++){
            if(!components.get(i).equals(otherComponents.get(i))){
        	return false;
            }
        }
        return true;
    }

    /** Returns the hash code of the path. */
    @Override
    public int hashCode()
    {
        int hashcode = 0;
        for(String string:components){
            hashcode += string.hashCode();
            hashcode *= 11;
        }
        return hashcode;
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
        String pathString = new String();
        if(this.isRoot()){
            pathString += "/";
        }
        for(String string:components){
            pathString += "/";
            pathString += string;
        }
        return pathString;
    }
}
