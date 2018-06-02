package com.code.challenge;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventReceiver implements Runnable {

    public ServerSocketChannel eventSocketChannel;
    public SocketChannel socketChannel;
    static HashMap<String, List<String>> usersMapping ;
    static List<Event> eventsList;

    public EventReceiver() throws IOException{
        eventSocketChannel = ServerSocketChannel.open();
        eventSocketChannel.socket().bind(new InetSocketAddress(Configuration.EVENT_SOURCE_PORT));
        usersMapping = new HashMap<String, List<String>>();
        eventsList = new ArrayList<Event>();
    }

    synchronized public void receiveEvent() throws IOException, InterruptedException{
        try{
            socketChannel = eventSocketChannel.accept();
            int len = 1, index, offsetLength;
            String strBuffer="";

            //Allocate buffer to read into from the socket
            ByteBuffer buffer = ByteBuffer.allocate(100);

            while ( len != -1 ) {

                //Read the events sent by Event Source
                len = socketChannel.read(buffer);
                System.out.println("length read from socket: " + len);

                //Convert the bytes into String
                buffer.flip();
                byte[] bytes = new byte[len];
                buffer.get(bytes, 0, len);
                String offset = new String(bytes);

                //Concatenate data from previous buffer
                offset = strBuffer.concat(offset);
                strBuffer = null;

                //Demarcate the event till last received '\n' character and parse the event
                index = offset.lastIndexOf("\n");
                offsetLength = offset.length();
                if(index < (offsetLength-1)){
                    strBuffer = offset.substring(index+1, offsetLength);
                    parseEvent(offset.substring(0, index));
                }else{
                    parseEvent(offset);
                }

                System.out.println("Event: " + offset.substring(0, index));
                buffer.clear();
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    public void run(){
        try{
            receiveEvent();
        }catch(IOException e){
            e.printStackTrace();
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }

    synchronized void parseEvent(String event){
        int index, idx, length, flag;
        String toUser = null, fromUser = null;

        for(String str: event.split("\\r?\\n")){
            Event eventData = new Event();
            String split[] = str.split("\\|");
            eventData.setPayload(str + "\\r\\n");
            eventData.setSequence(Long.parseLong(split[0]));
            eventData.setType(split[1]);
            length = str.length();

            flag = index = 0;

            while(flag != -1)
            {
                flag = str.indexOf("|", flag );
                if (flag != -1){
                    index++;
                    flag++;
                }
            }

            idx = str.lastIndexOf("|");

            if (index == 3){
                toUser = str.substring(idx + 1, length);
                eventData.setToUserId(toUser);
                int x = str.indexOf("|");
                fromUser = str.substring(str.indexOf("|", ++x ) + 1, idx);
                eventData.setFromUserId(fromUser);
            }

            if (index == 2){
                fromUser = str.substring(str.lastIndexOf("|") + 1, length);
                eventData.setFromUserId(fromUser);
                toUser = null;
            }
            List<String> list = null;
            if(!usersMapping.containsKey(fromUser)){
                list = new ArrayList<String>();
                list.add(toUser);
                usersMapping.put(fromUser, list);
            }
            else {
                list = usersMapping.get(fromUser);
                if(!list.contains(toUser)){
                    list.add(toUser);
                }
                usersMapping.put(fromUser, list);
            }

            eventsList.add(eventData);

            System.out.println("Users Mapping: " + usersMapping);

            System.out.println("event data List" + eventsList);
        }
    }




}
