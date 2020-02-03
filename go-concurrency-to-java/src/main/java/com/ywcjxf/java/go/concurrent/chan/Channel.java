package com.ywcjxf.java.go.concurrent.chan;

public interface Channel<T> extends SendChannel<T>,ReceiveChannel<T>{
    Object getNativeChannel();
    static <T> Channel<T> newChannel(int n){
        return new DefaultChannel<>(n);
    }

    static <T> Channel<T> newChannel(){
        return newChannel(0);
    }
}