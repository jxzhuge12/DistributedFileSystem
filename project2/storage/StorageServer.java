package storage;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.Files;

import common.*;
import rmi.*;
import naming.*;

/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */
public class StorageServer implements Storage, Command
{
    private Skeleton<Command> command;
    private Skeleton<Storage> storage;
    private File root;
    /** Creates a storage server, given a directory on the local filesystem, and
        ports to use for the client and command interfaces.

        <p>
        The ports may have to be specified if the storage server is running
        behind a firewall, and specific ports are open.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @param client_port Port to use for the client interface, or zero if the
                           system should decide the port.
        @param command_port Port to use for the command interface, or zero if
                            the system should decide the port.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root, int client_port, int command_port)
    {
        if(root == null) throw new NullPointerException("root is null");
        if(command_port == 0) command = new Skeleton(Command.class, this);
        else command = new Skeleton(Command.class, this, new InetSocketAddress(command_port));
        if(client_port == 0) storage = new Skeleton(Storage.class, this);
        else storage = new Skeleton(Storage.class, this, new InetSocketAddress(client_port));
        this.root = root;
    }

    /** Creats a storage server, given a directory on the local filesystem.

        <p>
        This constructor is equivalent to
        <code>StorageServer(root, 0, 0)</code>. The system picks the ports on
        which the interfaces are made available.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
     */
    public StorageServer(File root)
    {
        this(root, 0, 0);
    }

    /** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
        throws RMIException, UnknownHostException, FileNotFoundException
    {
        if (!root.exists() || root.isFile()) throw new FileNotFoundException("cannot find");
        command.start();
        storage.start();
        Command command_stub = Stub.create(Command.class, command, hostname);
        Storage storage_stub = Stub.create(Storage.class, storage, hostname);
        Path[] del = naming_server.register(storage_stub, command_stub, Path.list(root));
        for(int i = 0; i < del.length; i++) del[i].toFile(root).delete();
        prune(root);
    }

    private void prune(File f){
        if(!f.isDirectory()) return;
        File[] files = f.listFiles();
        for(int i = 0; i < files.length; i++) prune(files[i]);
        if(f.listFiles().length == 0) f.delete();
    }
    
    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    public void stop()
    {
        command.stop();
        storage.stop();
        stopped(null);
    }

    /** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code> if
                     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        File f = file.toFile(root);
        if(file.isRoot() || !f.exists() || f.isDirectory()) throw new FileNotFoundException("cannot find");
        return f.length();
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        File f = file.toFile(root);
        if(file.isRoot() || !f.exists() || f.isDirectory()) throw new FileNotFoundException("cannot find");
        if(length < 0 || offset < 0 || offset + length > f.length()) throw new IndexOutOfBoundsException("out of bound");
        return Arrays.copyOfRange(Files.readAllBytes(f.toPath()), (int)offset, length);
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        File f = file.toFile(root);
        if(file.isRoot() || !f.exists() || f.isDirectory()) throw new FileNotFoundException("cannot find");
        if(offset < 0) throw new IndexOutOfBoundsException("out of bound");
        RandomAccessFile writer = new RandomAccessFile(f, "rw");
        writer.seek(offset);
        writer.write(data);
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
        if (file.isRoot()) return false;
        File f = file.parent().toFile(root);
        if (f.exists() && !f.isDirectory()) return false;
        if (!f.exists()) f.mkdirs();
        try
        {
            return file.toFile(root).createNewFile();
        }
        catch(IOException e) { stopped(e); }
        return false;
    }

    @Override
    public synchronized boolean delete(Path path)
    {
        if(path.isRoot()) return false;
        File f = path.toFile(root);
        if (!f.exists()) return false;
        if (!f.isDirectory()) return f.delete();
        File[] files = f.listFiles();
        for(int i = 0; i < files.length; i++) delete(new Path(files[i].getPath().substring(root.toString().length())));
        f.delete();
        return true;
    }

    @Override
    public synchronized boolean copy(Path file, Storage server)
        throws RMIException, FileNotFoundException, IOException
    {
        byte[] filedata = (byte[])server.read(file, 0, (int)server.size(file));
        delete(file);
        if (!create(file)) return false;
        File f = file.toFile(root);
        write(file, 0, filedata);
        return true;
    }
}
