package com.code.challenge;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;;

public class SendClients implements Runnable {

    final static Logger logger = Logger.getLogger(SendClients.class);

    ExecutorService service;

    public SendClients(ExecutorService service)
    {
        this.service = service;
    }

    synchronized public void run(){
        sendEvents();
    }

    public void sendEvents(){
        Event event;
        String toUser, fromUser, msgType, payload;
        List<String> toUserList;
        SocketChannel socketChannel;

        int size = 1, timeout = 0;
        long end = System.currentTimeMillis() + 120000;

        try {
            do {
                if (size <= 0 ){
                    end = System.currentTimeMillis() + 120000; //waits to ensure no more data in the list
                }
                Thread.sleep(5000);
                synchronized (EventReceiver.eventsList) {
                    EventReceiver.eventsList.sort(Comparator.comparing(Event::getSequence));
                    ListIterator iterator = EventReceiver.eventsList.listIterator();
                    while (iterator.hasNext()) {
                        event = (Event) iterator.next();
                        toUser = event.getToUserId();
                        fromUser = event.getFromUserId();
                        toUserList = EventReceiver.usersMapping.get(fromUser);
                        msgType = event.getType();
                        payload = event.getPayload();

                        byte[] bytes = payload.getBytes();
                        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
                        buffer.put(payload.getBytes());

                        switch (msgType) {

                            case "F":
                            case "P":
                                //Only the `To User Id` should be notified
                                sendMsgFP(ClientsConnection.clientMap.get(toUser), buffer, toUser, msgType);
                                break;
                            case "U":
                                //No clients should be notified
                                logger.debug("Unfollow...No client is notified!!!");
                                break;

                            case "B":
                                //All connected *user clients* should be notified
                                sendBroadcast(buffer, msgType);
                                break;

                            case "S":
                                //All current followers of the `From User ID` should be notified
                                sendToFollowers(ClientsConnection.clientMap, buffer, toUserList, fromUser, msgType);
                                break;
                            default:
                                new RuntimeException("Probably corrupted data!!!");
                        }
                        iterator.remove();
                        EventReceiver.eventsList.sort(Comparator.comparing(Event::getSequence));
                        iterator = EventReceiver.eventsList.listIterator();
                        size = EventReceiver.eventsList.size();
                        logger.info("eventsList size# " + size);
                    }
                }
            }while(size > 0 || System.currentTimeMillis() < end);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
        logger.info("Notification service ended...");
    }

    public void sendMsgFP(SocketChannel socketChannel, ByteBuffer buffer, String toUser, String msgType){

        service.submit(() -> {
            try {
                if (socketChannel != null && socketChannel.isConnected()) {
                    buffer.flip();
                    socketChannel.write(buffer);
                    logger.debug("To User: " + toUser + " MsgType:" + msgType + " Client: " + socketChannel.toString());
                } else {
                    logger.debug("Client: " + toUser + " not found in connected users list or socket no longer connected");
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        });
    }

    public void sendBroadcast(ByteBuffer buffer, String msgType){
        service.submit(() -> {
            try{
                List<SocketChannel> allUserChannels = (List<SocketChannel>)ClientsConnection.clientMap.values();
                buffer.flip();
                for (SocketChannel sc : allUserChannels) {
                    if (sc != null && sc.isConnected()) {
                        buffer.flip();
                        sc.write(buffer);
                        logger.debug("MsgType " + msgType + " To Clients: " + sc.toString());
                    } else {
                        logger.debug("Socket no longer connected");
                    }
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        });
    }

    public void sendToFollowers(HashMap<String, SocketChannel> clientMap, ByteBuffer buffer, List<String> toUserList, String fromUser, String msgType){
        service.submit( () -> {
            try{
                for(String user: toUserList){
                    SocketChannel socketChannel = clientMap.get(user);
                    if (socketChannel != null && socketChannel.isConnected()) {
                        buffer.flip();
                        socketChannel.write(buffer);
                        logger.debug("From User: " + fromUser + " MsgType " + msgType + " Sent to Client: " + socketChannel.toString());
                    }
                    else{
                        logger.debug("Socket no longer connected");
                    }
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        });
    }
}


/*Event event;
        String toUser, fromUser, msgType, payload;
        List<String> toUserList;
        SocketChannel socketChannel;

        while(true){

            try {
                Thread.sleep(200);
                eventsList.sort(Comparator.comparing(Event::getSequence));
                ListIterator iterator = eventsList.listIterator();
                while (iterator.hasNext()) {
                    event = (Event) iterator.next();
                    toUser = event.getToUserId();
                    fromUser = event.getFromUserId();
                    toUserList = usersMapping.get(fromUser);
                    msgType = event.getType();
                    payload = event.getPayload();

                    byte[] bytes = payload.getBytes();
                    ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
                    buffer.put(payload.getBytes());

                    switch (msgType) {

                        case "F":
                        case "P":
                            //Only the `To User Id` should be notified
                            socketChannel = clientMap.get(toUser);
                            if (socketChannel != null && socketChannel.isConnected()) {
                                buffer.flip();
                                socketChannel.write(buffer);
                                logger.debug("To User: " + toUser + " MsgType:" + msgType + " Client: " + socketChannel.toString());
                            }
                            else{
                                logger.debug("Client: " + toUser + "not found in connected users list or socket no longer connected");
                            }

                            break;
                        case "U":
                            //No clients should be notified
                            logger.debug("Unfollow...No client is notified!!!");
                            break;

                        case "B":
                            //All connected *user clients* should be notified
                            List<SocketChannel> allUserChannels = (List<SocketChannel>)clientMap.values();
                            buffer.flip();
                            for (SocketChannel sc : allUserChannels) {
                                if(sc != null && sc.isConnected()){
                                    buffer.flip();
                                    sc.write(buffer);
                                    logger.debug("MsgType" + msgType + " To Clients: " + sc.toString());
                                }
                                else{
                                    logger.debug("Socket no longer connected");
                                }
                            }
                            break;

                        case "S":
                            //All current followers of the `From User ID` should be notified
                            for(String user: toUserList){
                                socketChannel = clientMap.get(user);
                                if (socketChannel != null && socketChannel.isConnected()) {
                                    buffer.flip();
                                    socketChannel.write(buffer);
                                    logger.debug("From User: " + fromUser + "MsgType " + msgType + " Sent to Client: " + socketChannel.toString());
                                }
                                else{
                                    logger.debug("Socket no longer connected");
                                }
                            }
                            break;

                        default:

                    }
                    iterator.remove();
                    this.clientMap = ClientsConnection.clientMap;
                    this.usersMapping = EventReceiver.usersMapping;
                    this.eventsList = EventReceiver.eventsList;
                    eventsList.sort(Comparator.comparing(Event::getSequence));
                    iterator = eventsList.listIterator();
                }
            }catch(IOException e){
                e.printStackTrace();
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }*/
