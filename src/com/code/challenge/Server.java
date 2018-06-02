package com.code.challenge;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Server {

    public static void main(String[] args) throws IOException{
        final ExecutorService service = Executors.newFixedThreadPool(5);
        ClientsConnection clientsConnection = new ClientsConnection();
        EventReceiver eventReceiver = new EventReceiver();
        SendClients sendClients = new SendClients();

        service.submit(eventReceiver);
        service.submit(clientsConnection);
        //service.submit(sendClients);

        //service.shutdown();
    }


}
