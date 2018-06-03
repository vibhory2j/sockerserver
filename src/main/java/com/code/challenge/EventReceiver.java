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
import org.apache.log4j.Logger;

public class EventReceiver implements Runnable {

    final static Logger logger = Logger.getLogger(EventReceiver.class);

    public ServerSocketChannel eventSocketChannel;
    public SocketChannel socketChannel;
    //Data structure to hold from-to user relationship
    volatile public static HashMap<String, List<String>> usersMapping;

    //List of all events received from event Source
    volatile public static List<Event> eventsList;

    public EventReceiver() throws IOException{
        eventSocketChannel = ServerSocketChannel.open();
        eventSocketChannel.socket().bind(new InetSocketAddress(Configuration.EVENT_SOURCE_PORT));
        usersMapping = new HashMap<String, List<String>>();
        eventsList = Collections.synchronizedList(new ArrayList<Event>());
    }

    synchronized public void receiveEvent() throws IOException, InterruptedException{
        try{
            socketChannel = eventSocketChannel.accept();
            int len = 1, index, offsetLength;
            StringBuilder strBuffer= new StringBuilder("");

            //Allocate buffer to read into from the socket
            ByteBuffer buffer = ByteBuffer.allocate(Configuration.BUFFER_SIZE);

            long end = System.currentTimeMillis() + 120000;;

            do {
                Thread.sleep(1000);

                //Read the events sent by Event Source
                len = socketChannel.read(buffer);
                logger.debug("Bytes read from socket: " + len);

                //Convert the bytes into String
                buffer.flip();
                byte[] bytes = new byte[len];
                buffer.get(bytes, 0, len);
                String temp = new String(bytes);
                StringBuilder offset = new StringBuilder(temp);

                //Concatenate data from previous buffer
                offset = strBuffer.append(offset);
                if( len > 0){ //retaining buffer in case data is missed on any subsquent hits.
                    strBuffer = new StringBuilder("");
                }

                //Demarcate the event till last received '\n' character and parse the event
                index = offset.lastIndexOf("\n");
                offsetLength = offset.length();
                if(index < (offsetLength-1)){
                    strBuffer.append(offset.substring(index+1, offsetLength));
                    parseEvent(offset.substring(0, index));
                }else{
                    parseEvent(offset.toString());
                }
                buffer.clear();

                if (len == -1 ){
                    end = System.currentTimeMillis() + 120000; //Waits for one minute in case no data in pipe
                }

            }while(len > 0 || System.currentTimeMillis() < end);
        }catch(IOException e){
            e.printStackTrace();
        }
        logger.info("Event Receiver service ended....");
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

    void parseEvent(String event){
        int index, idx, length, flag;
        String toUser = null, fromUser = null;

        for(String str: event.split("\\r?\\n")){
            Event eventData = new Event();
            String split[] = str.split("\\|");
            eventData.setPayload(str);
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

            synchronized (eventData){
                eventsList.add(eventData);
            }


            //logger.debug("User Mapping#: " + usersMapping);
            //logger.debug("Events List#: " + eventsList);
        }
    }




}
