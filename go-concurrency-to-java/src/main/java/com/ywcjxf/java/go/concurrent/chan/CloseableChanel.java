package com.ywcjxf.java.go.concurrent.chan;

public interface CloseableChanel {
    void close() throws ChannelAlreadyClosedException;
}
