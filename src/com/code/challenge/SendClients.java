package com.code.challenge;

import com.sun.deploy.util.SessionState;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;

public class SendClients implements Runnable {

    public HashMap<String, SocketChannel> clientMap;
    public HashMap<String, List<String>> usersMapping;
    static List<Event> eventsList;

    SendClients()
    {
        this.clientMap = ClientsConnection.clientMap;
        this.usersMapping = EventReceiver.usersMapping;
        this.eventsList = EventReceiver.eventsList;
    }

    public void run(){
        Event event;
        String toUser, fromUser, msgType, payload;
        List<String> toUserList;
        SocketChannel socketChannel;

        while(true){

            try {
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

                    System.out.println("payload: " + payload);

                    switch (msgType) {

                        case "F":
                        case "P":
                            //Only the `To User Id` should be notified

                            socketChannel = clientMap.get(toUser);
                            if (socketChannel != null && socketChannel.isConnected()) {
                                buffer.flip();
                                socketChannel.write(buffer);
                                System.out.println("To User: " + toUser + "Client: " + socketChannel.toString());
                            }
                            else{
                                System.out.println("Client: " + toUser + "not found in connected users list or socket no longer connected");
                            }

                            break;
                        case "U":
                            //No clients should be notified
                            System.out.println("Unfollow...No client is notified!!!");
                            break;

                        case "B":
                            //All connected *user clients* should be notified
                            List<SocketChannel> allUserChannels = (List<SocketChannel>)clientMap.values();
                            buffer.flip();
                            for (SocketChannel sc : allUserChannels) {
                                if(sc != null && sc.isConnected()){
                                    buffer.flip();
                                    sc.write(buffer);
                                    System.out.println("Payload send to Client: " + sc.toString());
                                }
                                else{
                                    System.out.println("Socket no longer connected");
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
                                    System.out.println("From User: " + fromUser + "Sent to Client: " + socketChannel.toString());
                                }
                                else{
                                    System.out.println("Socket no longer connected");
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
            }
        }
    }
}
