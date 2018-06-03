package com.code.challenge;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

public class ClientsConnection implements Callable<Integer>{

    final static Logger logger = Logger.getLogger(ClientsConnection.class);

    volatile public static HashMap<String, SocketChannel> clientMap = new HashMap<String, SocketChannel>();;
    public ServerSocketChannel clientSocketChannel;
    public SocketChannel socketChannel;

    public ClientsConnection() throws IOException{
        clientSocketChannel = ServerSocketChannel.open();
        clientSocketChannel.socket().bind(new InetSocketAddress(Configuration.CLIENTS_PORT));
        clientSocketChannel.configureBlocking(false);
    }

    public int acceptConnections() throws IOException{
        try{
            int len, timeout = 0;
            long end = 0;
            do{
                SocketChannel socketChannelClients = clientSocketChannel.accept();
                if (socketChannelClients == null){
                    if (timeout == 0){
                        timeout = 1;
                        end = System.currentTimeMillis() + 30000;
                    }
                }
                else{
                    timeout = 0;
                    end = System.currentTimeMillis() + 30000;
                    ByteBuffer buffer = ByteBuffer.allocate(100);
                    len = socketChannelClients.read(buffer);
                    if(len > 0)
                    {
                        buffer.flip();
                        byte[] bytes = new byte[len];
                        buffer.get(bytes, 0, len);
                        String str = new String(bytes).split("\\n")[0];
                        if(!clientMap.containsKey(str)){
                            clientMap.put(str,socketChannelClients);
                        }
                    }
                    logger.debug("Clients# Connected: " + clientMap.size());
                    buffer.clear();
                }
            }while(System.currentTimeMillis() < end);
        }catch(IOException e){
            e.printStackTrace();
        }
        logger.info("Client Connections service ended....");
        logger.debug("Clients Map: " + clientMap);
        return clientMap.size();
    }

    public Integer call(){
        int i = 0;
        try{
            i = acceptConnections();
        }catch(IOException e){
            e.printStackTrace();
        }
        return i;
    }
}
