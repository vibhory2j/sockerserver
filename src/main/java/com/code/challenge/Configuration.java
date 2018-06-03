package com.code.challenge;

public class Configuration {

    public static int EVENT_SOURCE_PORT = 9090;
    public static int CLIENTS_PORT = 9099;
    public static int BUFFER_SIZE = 2048;
    public static int THREAD_POOL_SIZE = 50;

    Configuration(){
        if(System.getenv("ESPORT") != null){
            EVENT_SOURCE_PORT = Integer.parseInt(System.getenv("ESPORT"));
        }

        if(System.getenv("CPORT") != null){
            CLIENTS_PORT = Integer.parseInt(System.getenv("CPORT"));
        }

        if(System.getenv("BUFFER_SIZE") != null){
            BUFFER_SIZE = Integer.parseInt(System.getenv("BSIZE"));
        }

        if(System.getenv("THREAD_POOL_SIZE") != null){
            THREAD_POOL_SIZE = Integer.parseInt(System.getenv("TSIZE"));
        }
    }
}
