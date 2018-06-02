package com.code.challenge;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;

public class ClientsConnection implements Runnable{

    public static HashMap<String, SocketChannel> clientMap = new HashMap<String, SocketChannel>();;
    public ServerSocketChannel clientSocketChannel;
    public SocketChannel socketChannel;

    public ClientsConnection() throws IOException{
        clientSocketChannel = ServerSocketChannel.open();
        clientSocketChannel.socket().bind(new InetSocketAddress(Configuration.CLIENTS_PORT));
        clientSocketChannel.configureBlocking(false);
    }

    public void acceptConnections() throws IOException{
        try{
            int len, timeout = 0;
            long end = 0;

            //while (System.currentTimeMillis() < end){
                len = 1;
                /*while( len != -1 )*/do{
                    SocketChannel socketChannelClients = clientSocketChannel.accept();
                    if (socketChannelClients == null){
                        if (timeout == 0){
                            timeout = 1;
                            end = System.currentTimeMillis() + 20000;
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
                        System.out.println("Clients Size: " + clientMap.size());
                        buffer.clear();
                    }
                }while(System.currentTimeMillis() < end);
//            }
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    public void run(){

        try{
            acceptConnections();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
}
