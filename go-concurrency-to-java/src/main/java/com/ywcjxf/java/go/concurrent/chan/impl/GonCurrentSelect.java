package com.ywcjxf.java.go.concurrent.chan.impl;

import com.ywcjxf.java.go.concurrent.chan.AbstractSelect;
import com.ywcjxf.java.go.concurrent.chan.ChannelAlreadyClosedException;
import com.ywcjxf.java.go.concurrent.chan.impl.third.io.github.anolivetree.goncurrent.Chan;
import com.ywcjxf.java.go.concurrent.chan.impl.third.io.github.anolivetree.goncurrent.Select;

import java.util.List;

public class GonCurrentSelect<T> extends AbstractSelect<T> {
    @Override
    public SelectResult<T> selectInternal(List<Register<T>> list, boolean nonBlocking) {
        Select select = new Select();
        for (Register<T> register:list){
            if(register.data==null){
                select.receive((Chan)(register.chan.getNativeChannel()));
            }else{
                select.send((Chan)(register.chan.getNativeChannel()),register.data);
            }
        }

        try {
            int i;
            if(nonBlocking){
                i = select.selectNonblock();
            }else{
                i = select.select();
            }

            if(i==-1){
                return null;
            }else{
                Register<T> register = list.get(i);
                return new SelectResult<>(i,register.chan,(T)select.getData(),register.data==null?SelectResult.RECEIVE:SelectResult.SEND);
            }
        }catch (Exception ex){
            throw new ChannelAlreadyClosedException(ex);
        }
    }
}
