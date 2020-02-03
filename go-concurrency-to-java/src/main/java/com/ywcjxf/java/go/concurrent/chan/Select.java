package com.ywcjxf.java.go.concurrent.chan;

import java.util.List;
import java.util.Objects;

public interface Select<T> {
    static <T> Select<T> newSelect(){
        return new DefaultSelect<>();
    }

    Select<T> register(ReceiveChannel<T> chan);
    Select<T> register(SendChannel<T> chan,T t);

    SelectResult<T> selectAfterRegister();
    SelectResult<T> trySelectAfterRegister();

    static <T> Register<T> send(SendChannel<T> chan,T t){
        Objects.requireNonNull(t);
        return new Register<>((Channel<T>)chan,t);
    }

    static <T> Register<T> receive(ReceiveChannel<T> chan){
        return new Register<>((Channel<T>)chan);
    }

    SelectResult<T> selectWithRegister(List<Register<T>> list);
    SelectResult<T> selectWithRegister(Register<T>... list);

    SelectResult<T> trySelectWithRegister(List<Register<T>> list);
    SelectResult<T> trySelectWithRegister(Register<T>... list);


    class Register<T>{
        public Channel<T> chan;
        public T data;

        public Register(Channel<T> chan, T data) {
            Objects.requireNonNull(data);

            this.chan = chan;
            this.data = data;
        }

        public Register(Channel<T> chan) {
            this.chan = chan;
        }
    }

    class SelectResult<T>{
        public int index;
        public Channel<T> chan;
        public T data;
        public int type;

        public static final int RECEIVE = 0;
        public static final int SEND = 1;

        public SelectResult(int index, Channel<T> chan, T data, int type) {
            this.index = index;
            this.chan = chan;
            this.data = data;
            this.type = type;
        }

        public boolean isReceive(){
            return type==RECEIVE;
        }
    }
}
