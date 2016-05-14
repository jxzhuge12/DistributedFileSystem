package naming;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Semaphore;

import rmi.*;
import common.*;
import storage.*;

/** Naming server.

    <p>
    Each instance of the filesystem is centered on a single naming server. The
    naming server maintains the filesystem directory tree. It does not store any
    file data - this is done by separate storage servers. The primary purpose of
    the naming server is to map each file name (path) to the storage server
    which hosts the file's contents.

    <p>
    The naming server provides two interfaces, <code>Service</code> and
    <code>Registration</code>, which are accessible through RMI. Storage servers
    use the <code>Registration</code> interface to inform the naming server of
    their existence. Clients use the <code>Service</code> interface to perform
    most filesystem operations. The documentation accompanying these interfaces
    provides details on the methods supported.

    <p>
    Stubs for accessing the naming server must typically be created by directly
    specifying the remote network address. To make this possible, the client and
    registration interfaces are available at well-known ports defined in
    <code>NamingStubs</code>.
 */
public class NamingServer implements Service, Registration
{
    private Node root;
    private HashMap<Storage, Command> storageServers;
    
    private Skeleton<Service> service;
    private Skeleton<Registration> registration;
    
    private class Node{
        public Node parent;
        public String name;
        public String path;
        public ArrayList<Node> children;
        public boolean isDirectory;
        public Storage storage;
        private int count;
        private boolean exclusive;
        private Semaphore run, mutex;
        private LinkedList<Boolean> isExclusive;
        
        public Node(Node parent, boolean isDirectory, String name, Storage storage){
            this.parent = parent;
            this.isDirectory = isDirectory;
            this.name = name;
            this.storage = storage;
            children = new ArrayList<Node>();
            path = parent == null ? "" : parent.path + "/" + name;
            count = 0;
            exclusive = false;
            isExclusive = new LinkedList<Boolean>();
            run = new Semaphore(1);
            mutex = new Semaphore(1);
        }
        
        public void lock(boolean exclusive) throws InterruptedException
        {
            try
            {
                mutex.acquire();
                if(count == 0){
                    run.acquire();
                    count++;
                    this.exclusive = exclusive;
                }
                else if(!this.exclusive && !exclusive && isExclusive.isEmpty()) count++;
                else {
                    isExclusive.add(exclusive);
                    mutex.release();
                    run.acquire();
                    mutex.acquire();
                    count++;
                    this.exclusive = exclusive;
                    isExclusive.remove();
                    if(!exclusive && !isExclusive.isEmpty() && isExclusive.peek() == false) run.release();
                }

                mutex.release();
            }
            catch(InterruptedException e) { stopped(e); }
        }
        
        public void unlock(boolean exclusive)
        {
            try
            {
                mutex.acquire();
                count--;
                if (count == 0) run.release();
                mutex.release(); 
            }
            catch(InterruptedException e) { stopped(e); }
        }
        
        public Node find(String child){
            if(!isDirectory) return null;
            for(int i = 0; i < children.size(); i++)
                if(children.get(i).name.equals(child)) return children.get(i);
            return null;
        }
        
        public Node add(String child){
            if(!isDirectory) return null;
            Node n = find(child);
            if(n == null) {
                n = new Node(this, true, child, null);
                children.add(n);
            }
            return n;
        }
        
        public boolean delete(Node n){
            if(!isDirectory) return false;
            return children.remove(n);
        }
    }
    
    private Node add(Path path, Storage storage){
        Node n = root;
        for(int i = 0; i < path.pathArray.size(); i++)
            n = n.add(path.pathArray.get(i));
        if(storage != null){
            n.isDirectory = false;
            n.storage = storage;
        }
        return n;
    }
    
    private boolean exist(Path path){
        Node n = root;
        for(int i = 0; i < path.pathArray.size(); i++){
            n = n.find(path.pathArray.get(i));
            if(n == null) break;
        }
        return n != null;
    }
   
    private Node find(Path path){
        Node n = root;
        for(int i = 0; i < path.pathArray.size(); i++){
            n = n.find(path.pathArray.get(i));
            if(n == null) break;
        }
        return n;
    }
    
    /** Creates the naming server object.

        <p>
        The naming server is not started.
     */
    public NamingServer()
    {
        root = new Node(null, true, "", null);
        storageServers = new HashMap<Storage, Command>();
        service = new Skeleton(Service.class, this, new InetSocketAddress(NamingStubs.SERVICE_PORT));
        registration = new Skeleton(Registration.class, this, new InetSocketAddress(NamingStubs.REGISTRATION_PORT));
    }

    /** Starts the naming server.

        <p>
        After this method is called, it is possible to access the client and
        registration interfaces of the naming server remotely.

        @throws RMIException If either of the two skeletons, for the client or
                             registration server interfaces, could not be
                             started. The user should not attempt to start the
                             server again if an exception occurs.
     */
    public synchronized void start() throws RMIException
    {
        service.start();
        registration.start();
    }

    /** Stops the naming server.

        <p>
        This method commands both the client and registration interface
        skeletons to stop. It attempts to interrupt as many of the threads that
        are executing naming server code as possible. After this method is
        called, the naming server is no longer accessible remotely. The naming
        server should not be restarted.
     */
    public void stop()
    {
        service.stop();
        registration.stop();
        stopped(null);
    }

    /** Indicates that the server has completely shut down.

        <p>
        This method should be overridden for error reporting and application
        exit purposes. The default implementation does nothing.

        @param cause The cause for the shutdown, or <code>null</code> if the
                     shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following public methods are documented in Service.java.
    @Override
    public void lock(Path path, boolean exclusive) throws FileNotFoundException
    {
        if(!exist(path)) throw new FileNotFoundException("cannot find");
        try{
            Node n = root;
            if(path.isRoot()) {
                n.lock(exclusive);
                return;
            }
            n.lock(false);
            for(int i = 0; i < path.pathArray.size() - 1; i++){
                n = n.find(path.pathArray.get(i));
                n.lock(false);
            }
            n = n.find(path.pathArray.get(path.pathArray.size() - 1));
            n.lock(exclusive);
        }
        catch(InterruptedException e){ stopped(e); }
    }

    @Override
    public void unlock(Path path, boolean exclusive)
    {
        Node n = find(path);
        if(n == null) throw new IllegalArgumentException("cannot find");
        n.unlock(exclusive);
        while(n != root){
            n = n.parent;
            n.unlock(false);
        }
    }

    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
        lock(path, false);
        if (path == null) throw new NullPointerException("null");
        Node n = find(path);
        if (n == null) throw new FileNotFoundException("directory");
        unlock(path, false);
        return n.isDirectory;
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {
        lock(directory, false);
        if (directory == null) throw new NullPointerException("null");
        Node n = find(directory);
        if (n == null) throw new FileNotFoundException("cannot find");
        if (!n.isDirectory) throw new FileNotFoundException("file");
        String[] r = new String[n.children.size()];
        for(int i = 0; i < n.children.size(); i++) r[i] = n.children.get(i).name;
        unlock(directory, false);
        return r;
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
        if (storageServers.isEmpty()) throw new IllegalStateException("no storage");
        if (file == null) throw new NullPointerException("null");
        if (file.isRoot()) return false;
        Path parent = file.parent();
        lock(parent, true);
        Node n = find(parent);
        if(n == null || !n.isDirectory) {
            unlock(parent, true);
            throw new FileNotFoundException("null");
        }
        if (find(file) != null) {
            unlock(parent, true);
            return false;
        }
        Random rand = new Random();
        int p = rand.nextInt(storageServers.size());
        Storage[] storages = new Storage[storageServers.size()];
        storageServers.keySet().toArray(storages);
        storageServers.get(storages[p]).create(file);
        if(add(file, storages[p]) == null) {
            unlock(parent, true);
            return false;
        }
        else {
            unlock(parent, true);
            return true;
        }
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException
    {
        if (directory == null) throw new NullPointerException("null");
        if (directory.isRoot()) return false;
        Path parent = directory.parent();
        lock(parent, true);
        Node n = find(parent);
        if(n == null || !n.isDirectory) {
            unlock(parent, true);
            throw new FileNotFoundException("null");
        }
        if(find(directory) != null || add(directory, null) == null) {
            unlock(parent, true);
            return false;
        }
        else {
            unlock(parent, true);
            return true;
        }
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException
    {
        if (path == null) throw new NullPointerException("null");
        lock(path, true);
        Node n = find(path);
        try
        {
            storageServers.get(n.storage).delete(path);
        }
        catch(RMIException e) { stopped(e); }
        boolean result = n.parent.delete(n);
        unlock(path, true);
        return result;
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException
    {
        if (file == null) throw new NullPointerException("null");
        lock(file, false);
        if (!exist(file)) throw new FileNotFoundException("not found");
        Node n = find(file);
        if (n.isDirectory) throw new FileNotFoundException("directory");
        Storage storage = n.storage;
        unlock(file, false);
        return storage;
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files)
    {
        if(client_stub == null || command_stub == null || files == null) throw new NullPointerException("null pointer");
        if (storageServers.containsKey(client_stub)) throw new IllegalStateException("already registered");
        Path r = new Path();
        try{
            lock(r, true);
        }
        catch(FileNotFoundException e) { stopped(e); }
        storageServers.put(client_stub, command_stub);
        ArrayList<Path> toDel = new ArrayList<Path>();
        for(int i = 0; i < files.length; i++){
            if(files[i].toString() == "/") continue;
            if(exist(files[i])) toDel.add(files[i]);
            else add(files[i], client_stub);
        }
        unlock(r, true);
        Path[] result = new Path[toDel.size()];
        return toDel.toArray(result);
    }
}
