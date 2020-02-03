package com.ywcjxf.java.go.concurrent.chan;

import java.util.Iterator;
import java.util.Objects;

public abstract class AbstractChannel<T> implements Channel<T> {
    public AbstractChannel() {
    }

    public AbstractChannel(int i) {
    }

    @Override
    public void send(T t) throws ChannelAlreadyClosedException {//子类要调用这个方法
        Objects.requireNonNull(t);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private T t;
            @Override
            public boolean hasNext(){
                T tmp = receive();
                if(tmp!=null){
                    t = tmp;
                    return true;
                }else{
                    return false;//也就是这里是channel 关闭后还会把channel中的全部读完
                }
            }

            @Override
            public T next() {
                T tmp = t;
                t=null;
                return tmp;
            }
        };
    }
}
