package com.code.challenge.tests;

import com.code.challenge.ClientsConnection;
import com.code.challenge.Configuration;
import com.code.challenge.EventReceiver;
import com.code.challenge.SendClients;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientsConnectionTest {

    @Test
    public void TestConnection() throws IOException, InterruptedException{
        ExecutorService service = Executors.newFixedThreadPool(5);
        ClientsConnection clientsConnection = new ClientsConnection();
        EventReceiver eventReceiver = new EventReceiver();
        SendClients sendClients = new SendClients(service);
        ByteBuffer buffer;

        //start server sockets
        service.submit(clientsConnection);
        service.submit(eventReceiver);

        Thread.sleep(1000);

        //Create client socket for Event Source connection
        SocketChannel eventSource = SocketChannel.open(new InetSocketAddress("localhost", 9090));

        //Create client socket for Clients notification connection
        SocketChannel clients = SocketChannel.open(new InetSocketAddress("localhost", 9099));

        assertEquals("Test for socket connections for Event Source: ", true , eventSource.isConnected());
        assertEquals("Test for socket connections for Clients: ", true , clients.isConnected());

        //Announcing client Id
        String clientId = new String("123456\n");
        byte[] bytes = clientId.getBytes();
        buffer = ByteBuffer.allocate(clientId.length());
        buffer.put(bytes);
        buffer.flip();
        while(clients.write(buffer) <= 0){
        }

        Thread.sleep(1000);

        System.out.println("clientMap: " + ClientsConnection.clientMap);
        assertEquals("Test for announcing client Id's: ", true , ClientsConnection.clientMap.containsKey(clientId.split("\\n")[0]));
        buffer.clear();

        //Sending events
        String events = new String("666|F|60|123456\r\n");
        buffer = ByteBuffer.allocate(events.length());
        buffer.put(events.getBytes());
        buffer.flip();
        System.out.println("Bytes written" + eventSource.write(buffer));
        Thread.sleep(1000);
        System.out.println("Users Mapping: " + EventReceiver.usersMapping);
        assertEquals("Test for sending events: ", true, EventReceiver.usersMapping.containsKey("60"));
        assertEquals("Test for sending events: ", 666, EventReceiver.eventsList.get(0).getSequence());
        buffer.clear();

        events = new String("542532|B\r\n43|P|32|123456\r\n");
        buffer = ByteBuffer.allocate(events.length());
        buffer.put(events.getBytes());
        buffer.flip();
        eventSource.write(buffer);
        Thread.sleep(1000);
        assertEquals("Test for sending events: ", true, EventReceiver.usersMapping.containsKey("32"));
        assertEquals("Test for sending events: ", 542532, EventReceiver.eventsList.get(1).getSequence());
        buffer.clear();

        //Sending notifications
        buffer = ByteBuffer.allocate(100);
        service.submit(sendClients);
        Thread.sleep(1000);
        //Receiving notifications
        int len = clients.read(buffer);
        buffer.flip();
        bytes = new byte[len];
        buffer.get(bytes, 0, len);
        buffer.flip();
        events = new String(bytes);
        assertEquals("Test for receiving events: ", true , events.indexOf("B", 0) >= 0 || events.indexOf("F", 0) >= 0 || events.indexOf("P", 0) >= 0);

    }



}
