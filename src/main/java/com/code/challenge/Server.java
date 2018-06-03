package com.code.challenge;

import org.apache.log4j.Logger;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.code.challenge.Configuration.THREAD_POOL_SIZE;

public class Server {

    final static Logger logger = Logger.getLogger(Server.class);

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException{

        Configuration config = new Configuration();
        ExecutorService service = Executors.newFixedThreadPool(Configuration.THREAD_POOL_SIZE);
        ClientsConnection clientsConnection = new ClientsConnection();
        EventReceiver eventReceiver = new EventReceiver();
        SendClients sendClients = new SendClients(service);

        logger.info("Starting Socket Server....");

        //Service that listens to the events
        service.submit(eventReceiver);

        //Service that listens for client connections and ID announcement
        Future<Integer> future = service.submit(clientsConnection);
        future.get();
        logger.info("All clients connected!!!");

        //Service that sends notifications to the clients
        service.submit(sendClients);

        logger.info("Socket Server Started....");
    }


}
