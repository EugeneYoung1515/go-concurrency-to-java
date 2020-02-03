package com.ywcjxf.java.go.concurrent.chan;

public interface SendChannel<T> extends CloseableChanel {
    void send(T t) throws ChannelAlreadyClosedException;
}
