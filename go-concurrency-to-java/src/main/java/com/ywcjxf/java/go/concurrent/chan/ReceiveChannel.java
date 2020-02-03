package com.ywcjxf.java.go.concurrent.chan;

public interface ReceiveChannel<T> extends Iterable<T>{
    T receive();
}
