package com.ywcjxf.java.go.concurrent.chan;

public class ChannelAlreadyClosedException extends RuntimeException {
    public ChannelAlreadyClosedException() {
    }

    public ChannelAlreadyClosedException(String message) {
        super(message);
    }

    public ChannelAlreadyClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChannelAlreadyClosedException(Throwable cause) {
        super(cause);
    }
}
