package com.code.challenge;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {

    public HashSet<String> connectedClients = new HashSet<>();

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        final ServerSocket serverEventsSocket = new ServerSocket(Configuration.EVENT_SOURCE_PORT);
        //final ServerSocket serverClientsSocket = new ServerSocket(Configuration.CLIENTS_PORT);
        ClientsConnection clientsConnection = new ClientsConnection();
        final ExecutorService service = Executors.newFixedThreadPool(10);
        EventReceiver eventReceiver = new EventReceiver();

            Runnable task1 = () -> {
                try {
                    Socket clientEventsSocket = serverEventsSocket.accept();
                    InetSocketAddress remote = (InetSocketAddress) clientEventsSocket.getRemoteSocketAddress();
                    System.out.println("Event Source Connection from port:" + remote.getPort() + " from Host:" + remote.getHostName());
                    DataInputStream in = new DataInputStream(clientEventsSocket.getInputStream());

                    int len = 1;
                    while (len >= 0) {
                        byte buffer[] = new byte[1024];
                        len = in.read(buffer, 0, 1024);
                        if (len > 0) {
                            String s = new String(buffer);
                            System.out.println("Length: " + len);
                            System.out.println("Buffer: " + s);
                        }

                    }
                }catch(IOException e){
                    e.printStackTrace();
                }
            };

            /*Runnable task2 = () -> {
               while(true){
                   try{
                       Socket clientSocket = serverClientsSocket.accept();
                       InetSocketAddress remote = (InetSocketAddress) clientSocket.getRemoteSocketAddress();
                       System.out.println("Clients Connection from port:" + remote.getPort() + " from Host:" + remote.getHostName());

                       DataInputStream in = new DataInputStream(clientSocket.getInputStream());

                       int len = 1;
                       while (len >= 0) {
                           byte buffer[] = new byte[10];
                           len = in.read(buffer, 0, 10);

                           if (len > 0) {
                               String s = new String(buffer);
                               System.out.println("Length: " + len);
                               System.out.println("Buffer: " + s);
                           }
                       }
                   }catch(IOException e){
                       e.printStackTrace();
                   }
               }
            };*/
            Future future;
            future = service.submit(task1);
        System.out.println("Submitted Task 1: ");
            //System.out.println("Future task1: " + future.get() );
            future = service.submit(clientsConnection);
        System.out.println("Submitted Task 2: ");
            //System.out.println("Future task2: " + future.get() );
    }
}
