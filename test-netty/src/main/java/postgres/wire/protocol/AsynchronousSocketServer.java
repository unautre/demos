package postgres.wire.protocol;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class AsynchronousSocketServer {
    private static final String HOST = "localhost";
    private static final int PORT = 5432;

    public static void main(String[] args) throws Exception {
//        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    	ExecutorService executor = Executors.newCachedThreadPool();
        AsynchronousChannelGroup group = AsynchronousChannelGroup.withThreadPool(executor);

        try (AsynchronousServerSocketChannel server = AsynchronousServerSocketChannel.open(group)) {
            server.bind(new InetSocketAddress(HOST, PORT));
            System.out.println("[SERVER] Listening on " + HOST + ":" + PORT);

            for (;;) {
                Future<AsynchronousSocketChannel> future = server.accept();
                AsynchronousSocketChannel client = future.get();
                System.out.println("[SERVER] Accepted connection from " + client.getRemoteAddress());
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                client.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer result, ByteBuffer attachment) {
                        attachment.flip();
                        if (result != -1) {
                            onMessageReceived(client, attachment);
                        }
                        attachment.clear();
                        client.read(attachment, attachment, this);
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                        System.err.println("[SERVER] Failed to read from client: " + exc);
                        exc.printStackTrace();
                    }
                });
            }
        }
    }

    private static void onMessageReceived(AsynchronousSocketChannel client, ByteBuffer buffer) {
        System.out.println("[SERVER] Received message from client: " + client);
        System.out.println("[SERVER] Buffer: " + buffer);
        System.out.println(hexDump(buffer));

        // First, write 'N' for SSL negotiation
        ByteBuffer response = ByteBuffer.allocate(1);
        response.put((byte) 'N');
        response.flip();
        Future<Integer> writeResult = client.write(response);

        // Then, write AuthenticationOk
        ByteBuffer authOk = ByteBuffer.allocate(9);
        authOk.put((byte) 'R'); // 'R' for AuthenticationOk
        authOk.putInt(8); // Length
        authOk.putInt(0); // AuthenticationOk
        authOk.flip();
        writeResult = client.write(authOk);

        // Then, write BackendKeyData
        ByteBuffer backendKeyData = ByteBuffer.allocate(17);
        backendKeyData.put((byte) 'K'); // Message type
        backendKeyData.putInt(12); // Message length
        backendKeyData.putInt(1234); // Process ID
        backendKeyData.putInt(5678); // Secret key
        backendKeyData.flip();
        writeResult = client.write(backendKeyData);

        // Then, write ReadyForQuery
        ByteBuffer readyForQuery = ByteBuffer.allocate(6);
        readyForQuery.put((byte) 'Z'); // 'Z' for ReadyForQuery
        readyForQuery.putInt(5); // Length
        readyForQuery.put((byte) 'I'); // Transaction status indicator, 'I' for idle
        readyForQuery.flip();
        writeResult = client.write(readyForQuery);

        try {
            writeResult.get();
        } catch (Exception e) {
            System.err.println("[SERVER] Failed to write to client: " + e);
        }
    }
    
    public static String hexDump(ByteBuffer buffer) {
        byte[] data = new byte[buffer.remaining()];
        int pos = buffer.position();
        buffer.get(data);
        buffer.position(pos);
        StringBuilder buf = new StringBuilder(buffer.remaining() + 22);
        for (byte b : data)
            addHexByte(buf, b);

        return buf.toString();
    }

    private static void addHexByte(StringBuilder buf, byte b) {
        final String HEX_VALUES = "0123456789ABCDEF";
        
        buf.append(HEX_VALUES.charAt((b & 0xF0) >> 4)).append(HEX_VALUES.charAt((b & 0x0F)));
    }
}

class MainSimplest {
    public static void main(String[] args) throws Exception {
        AsynchronousSocketServer.main(args);
    }
}