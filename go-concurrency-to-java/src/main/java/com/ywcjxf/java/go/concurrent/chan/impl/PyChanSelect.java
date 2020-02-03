package com.ywcjxf.java.go.concurrent.chan.impl;

import com.ywcjxf.java.go.concurrent.chan.AbstractSelect;
import com.ywcjxf.java.go.concurrent.chan.Channel;
import com.ywcjxf.java.go.concurrent.chan.ChannelAlreadyClosedException;
import com.ywcjxf.java.go.concurrent.chan.impl.third.pychan.GenericsChan;

import java.util.ArrayList;
import java.util.List;

public class PyChanSelect<T> extends AbstractSelect<T> {
    @Override
    public SelectResult<T> selectInternal(List<Register<T>> list, boolean nonBlocking) {
        List<GenericsChan<T>> consumers = new ArrayList<>(10);
        List<GenericsChan.Holder<T>> producers  = new ArrayList<>(10);

        for (Register<T> r:list){
            if(r.data==null){
                consumers.add((GenericsChan<T>)(r.chan.getNativeChannel()));
            }else{
                producers.add(new GenericsChan.Holder<T>((GenericsChan<T>)(r.chan.getNativeChannel()),r.data));
            }
        }

        try {
            GenericsChan.Holder<T> holder;
            if(nonBlocking){
                holder =  GenericsChan.tryChanSelect(consumers,producers);
            }else{
                holder =  GenericsChan.chanSelect(consumers,producers);
            }

            if(holder==null){
                return null;
            }else{
                int i = -1;
                int type = -1;
                Channel<T> channel = null;
                for (int j = 0; j < list.size(); j++) {
                    Channel<T> chan = list.get(j).chan;
                    if((GenericsChan<T>)chan.getNativeChannel()==holder.chan){
                        //这里其实有问题
                        //如果一个select同时监听两个channel 如果这两个channel是同一个
                        //这里选出的都会是第一个

                        i = j;
                        channel = chan;
                        if(list.get(i).data==null){
                            type = SelectResult.RECEIVE;
                        }else{
                            type = SelectResult.SEND;
                        }
                        break;
                    }
                }

                return new SelectResult<>(i,channel,holder.value,type);
            }
        }catch (Exception ex){
            throw new ChannelAlreadyClosedException(ex);
        }
    }
}
