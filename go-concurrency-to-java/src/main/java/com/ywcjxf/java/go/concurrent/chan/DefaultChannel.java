package com.ywcjxf.java.go.concurrent.chan;

import com.ywcjxf.java.go.concurrent.chan.impl.GonCurrentChannel;

public class DefaultChannel<T> extends GonCurrentChannel<T> {
    public DefaultChannel() {
        super();
    }

    public DefaultChannel(int i) {
        super(i);
    }
}
