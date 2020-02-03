package com.ywcjxf.java.go.concurrent.util;

//给闭包使用外部变量使用
public class Holder<T> {
    private T t;
    public void set(T t){
        this.t = t;
    }
    public T get(){
        return t;
    }
}
