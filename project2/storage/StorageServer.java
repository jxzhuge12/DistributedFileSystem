package storage;

import java.io.*;
import java.net.*;

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
    File root;
    Skeleton<Storage> client_skeleton;
    Skeleton<Command> command_skeleton;
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
        // throw new UnsupportedOperationException("not implemented");
        if (root == null)
        {
            throw new NullPointerException("root directory is null");
        }
        this.root = root;
        if (client_port > 0)
        {
            InetSocketAddress client_address = new InetSocketAddress(client_port);
            client_skeleton = new Skeleton<Storage>(Storage.class, this, client_address);
        }
        else
        {
            client_skeleton = new Skeleton<Storage>(Storage.class, this);
        }
        if (command_port > 0)
        {
            InetSocketAddress command_address = new InetSocketAddress(command_port);
            command_skeleton = new Skeleton<Command>(Command.class, this, command_address);
        }
        else
        {
            command_skeleton = new Skeleton<Command>(Command.class, this);
        }
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
        // throw new UnsupportedOperationException("not implemented");
        if (root == null)
        {
            throw new NullPointerException("root directory is null");
        }
        StorageServer(root, 0, 0);
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
        // throw new UnsupportedOperationException("not implemented");
        if (!root.exists() || root.isFile())
        {
            throw new FileNotFoundException("the root directory does not exist or is a file");
        }
        client_skeleton.start();
        command_skeleton.start();
        Storage client_stub = Stub.create(Storage.class, client_skeleton, hostname);
        Command command_stub = Stub.create(Command.class, command_skeleton, hostname);
        Path[] files = Path.list(root);
        Path[] delete_files = naming_server.register(client_stub, command_stub, files);
        for (int i = 0; i < delete_files.length; i++)
        {
            p.toFile(root).delete();
            while (!p.isroot())
            {
                p = p.parent();
                File temp_file = p.toFile(root);
                if (temp_file.list().length == 0)
                {
                    temp_file.delete();
                }
                else
                {
                    break;
                }
            }
        }
    }

    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    public void stop()
    {
        // throw new UnsupportedOperationException("not implemented");
        client_skeleton.stop();
        command_skeleton.stop();
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
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized boolean delete(Path path)
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public synchronized boolean copy(Path file, Storage server)
        throws RMIException, FileNotFoundException, IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }
}
