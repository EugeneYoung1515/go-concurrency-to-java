package com.ywcjxf.java.go.concurrent.chan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class AbstractSelect<T> implements Select<T> {
    private List<Register<T>> list = new ArrayList<>(10);

    @Override
    public Select<T> register(ReceiveChannel<T> chan) {
        list.add(Select.receive(chan));
        return this;
    }

    @Override
    public Select<T> register(SendChannel<T> chan, T t) {
        Objects.requireNonNull(t);
        list.add(Select.send(chan,t));
        return this;
    }

    @Override
    public SelectResult<T> selectAfterRegister() {
        //System.out.println(list);
        return selectInternal(list,false);
    }

    @Override
    public SelectResult<T> trySelectAfterRegister() {
        return selectInternal(list,true);
    }

    @Override
    public SelectResult<T> selectWithRegister(List<Register<T>> list) {
        return selectInternal(list,false);
    }

    @Override
    public SelectResult<T> selectWithRegister(Register<T>... list) {
        return selectInternal(Arrays.asList(list),false);
    }

    @Override
    public SelectResult<T> trySelectWithRegister(List<Register<T>> list) {
        return selectInternal(list,true);
    }

    @Override
    public SelectResult<T> trySelectWithRegister(Register<T>... list) {
        return selectInternal(Arrays.asList(list),true);
    }

    public abstract SelectResult<T> selectInternal(List<Register<T>> list,boolean nonBlocking);
}
