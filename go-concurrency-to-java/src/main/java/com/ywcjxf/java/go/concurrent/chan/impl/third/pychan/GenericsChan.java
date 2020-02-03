package com.ywcjxf.java.go.concurrent.chan.impl.third.pychan;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class GenericsChan<T> {
    private static class WishGroup<T>{
        private Wish<T> fullFilledBy = null;
        private ReentrantLock reentrantLock = new ReentrantLock();
        private Condition condition = reentrantLock.newCondition();
        private List<Wish<T>> wishes = new ArrayList<>(10);

        public boolean fullFilled(){
            return fullFilledBy!=null;
        }
    }

    private static int WISH_PRODUCE =0;
    private static int WISH_CONSUME = 1;

    private static class Wish<T>{
        private WishGroup<T> group;
        private int kind;
        private GenericsChan<T> chan;
        private T value;
        private boolean closed = false;

        public Wish(WishGroup<T> group, int kind, GenericsChan<T> chan, T value) {//Object value 可省略
            this.group = group;
            this.kind = kind;
            this.chan = chan;
            this.value = value;

            this.group.wishes.add(this);
        }

        public boolean fullFilled(){
            return group.fullFilled();
        }

        public T fullFill(T value,boolean closed){//Object value 可省略 boolean closed可省略
            assert !fullFilled();
            this.closed=closed;
            group.fullFilledBy = this;
            group.condition.signal();
            if(kind==WISH_PRODUCE){
                return this.value;
            }else{

                //原来是这样
                //this.value = value;
                //return null;

                if(closed){

                }else{
                    this.value = value;
                }
                return null;
            }
        }
    }

    private static class RingBuffer<T>{
        private T[] buf;
        private int nextPop = 0;
        private int len = 0;

        public RingBuffer(int bufLen) {
            this.buf = (T[])new Object[bufLen];
        }

        public int cap(){
            return buf.length;
        }

        public void push(T value){
            if(len==buf.length){
                throw new ArrayIndexOutOfBoundsException();
            }
            int nextPush = (this.nextPop+len)%buf.length;
            buf[nextPush] = value;
            len+=1;
        }

        public T pop(){
            if(len==0){
                throw new ArrayIndexOutOfBoundsException();
            }
            T value = buf[nextPop];
            buf[nextPop] = null;
            nextPop = (nextPop+1)%buf.length;
            len-=1;
            return value;
        }

        public int len(){
            return len;
        }

        public boolean empty(){
            return len==0;
        }

        public boolean full(){
            return len ==buf.length;
        }
    }

    private ReentrantLock lock = new ReentrantLock();
    private boolean closed = false;
    private RingBuffer<T> buf;
    private Deque<Wish<T>> waitingProducers = new LinkedList<>();
    private Deque<Wish<T>> waitingConsumers = new LinkedList<>();

    public GenericsChan() {
        this(0);
    }

    public GenericsChan(int bufLen) {//int bufLen默认值
        if(bufLen>0){
            buf = new RingBuffer<>(bufLen);
        }
    }

    private T fullFillWaitingProducer(){
        while (true){
            if(!waitingProducers.isEmpty()){
                Wish<T> productWish = waitingProducers.removeFirst();
                productWish.group.reentrantLock.lock();
                try {
                    if(!productWish.group.fullFilled()){
                        return productWish.fullFill(null,false);
                    }
                }finally {
                    productWish.group.reentrantLock.unlock();
                }
            }else{
                throw new RuntimeException("empty");
            }
        }
    }

    public T getNoWait(){
        if(buf!=null && !buf.empty()){
            T value = buf.pop();
            try {
                T produced = fullFillWaitingProducer();
                buf.push(produced);
            }catch (Exception ex){

            }
            return value;
        }else{
            return fullFillWaitingProducer();
        }
    }

    public void putNoWait(T value){
        while (true){
            if(!waitingConsumers.isEmpty()){
                Wish<T> consumeWish = waitingConsumers.removeFirst();
                consumeWish.group.reentrantLock.lock();
                try {
                    if(!consumeWish.group.fullFilled()){
                        consumeWish.fullFill(value,false);
                        return;
                    }
                }finally {
                    consumeWish.group.reentrantLock.unlock();
                }
            }else if(buf!=null && !buf.full()){
                buf.push(value);
                return;
            }else{
                throw new RuntimeException("full");
            }
        }
    }

    public T get(){
        return get(0);
    }

    public T get(long timeout){
        long timeoutDeadLine =0L;
        if(timeout!=0L){
            timeoutDeadLine = System.currentTimeMillis()+timeout;
        }

        WishGroup<T> group;
        Wish<T> wish;
        lock.lock();
        try {
            try {
                return getNoWait();
            }catch (RuntimeException ex){

            }

            if(closed){
                return null;//channel 关闭后且没值返回null
                //throw new RuntimeException("closed");
            }

            if(timeout!=0L && timeout<=0L){
                throw new RuntimeException("timeout");
            }

            group = new WishGroup<>();
            wish = new Wish<>(group,WISH_CONSUME,this,null);
            waitingConsumers.addLast(wish);
        }finally {
            lock.unlock();
        }

        group.reentrantLock.lock();
        try {
            while (!group.fullFilled()){
                if(timeout==0L){
                    try {
                        group.condition.await();
                    }catch (InterruptedException ex){
                        ex.printStackTrace();
                    }
                }else{
                    try {
                        group.condition.await(timeoutDeadLine-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                        if(System.currentTimeMillis()>=timeoutDeadLine){
                            if(!group.fullFilled()){
                                waitingConsumers.remove(wish);
                                throw new RuntimeException("timeout");
                            }
                        }
                    }catch (InterruptedException ex){
                        ex.printStackTrace();
                    }
                }
            }
        }finally {
            group.reentrantLock.unlock();
        }

        /*
        if(wish.closed){//原来放在这里
            return null;//?
            //throw new RuntimeException("closed");
        }
        */

        //要不要放到锁里
        return wish.value;
    }

    public void put(T value){
        put(value,0);
    }

    public void put(T value,long timeout){
        long timeoutDeadLine =0L;
        if(timeout!=0L){
            timeoutDeadLine = System.currentTimeMillis()+timeout;
        }

        WishGroup<T> group;
        Wish<T> wish;
        lock.lock();
        try {
            if(closed){
                throw new RuntimeException("closed");
            }

            try {
                putNoWait(value);
                return;
            }catch (Exception ex){

            }

            if(timeout!=0L && timeout<=0L){
                throw new RuntimeException("timeout");
            }

            group = new WishGroup<>();
            wish = new Wish<>(group,WISH_PRODUCE,this,value);
            waitingProducers.addLast(wish);
        }finally {
            lock.unlock();
        }

        group.reentrantLock.lock();
        try {
            while (!group.fullFilled()){
                if(timeout==0L){
                    try {
                        group.condition.await();
                    }catch (InterruptedException ex){
                        ex.printStackTrace();
                    }
                }else{
                    try {
                        group.condition.await(timeoutDeadLine-System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                        if(System.currentTimeMillis()>=timeoutDeadLine){
                            if(!group.fullFilled()){
                                waitingProducers.remove(wish);
                                throw new RuntimeException("timeout");
                            }
                        }
                    }catch (InterruptedException ex){
                        ex.printStackTrace();
                    }
                }
            }
        }finally {
            group.reentrantLock.unlock();
        }

        if(wish.closed){
            throw new RuntimeException("closed");
        }
    }

    public void close(){
        Deque<Wish<T>> wishes = new LinkedList<>();
        lock.lock();
        try {
            if(closed){
                throw new RuntimeException("double closed");
            }
            closed = true;

            wishes.addAll(waitingProducers);
            wishes.addAll(waitingConsumers);
        }finally {
            lock.unlock();
        }

        for(Wish<T> wish:wishes){
            wish.group.reentrantLock.lock();
            try {
                if(!wish.fullFilled()){
                    wish.fullFill(null,true);
                }
            }finally {
                wish.group.reentrantLock.unlock();
            }
        }
    }

    public boolean closed(){
        lock.lock();
        try {
            return closed && !waitingProducers.isEmpty();
        }finally {
            lock.unlock();
        }
    }

    public static class Holder<T>{
        public GenericsChan<T> chan;
        public T value;

        public Holder(GenericsChan<T> chan, T value) {
            this.chan = chan;
            this.value = value;
        }
    }

    public static <T> Holder<T> chanSelect(List<GenericsChan<T>> consumers, List<Holder<T>> producers){
        return chanSelect(consumers,producers,0);
    }

    public static <T> Holder<T> chanSelect(List<GenericsChan<T>> consumers, List<Holder<T>> producers, long timeout){
        return tryChanSelect(consumers,producers,timeout,false);
    }

    public static <T> Holder<T> tryChanSelect(List<GenericsChan<T>> consumers, List<Holder<T>> producers){
        return tryChanSelect(consumers,producers,0,true);
    }

    public static <T> Holder<T> tryChanSelect(List<GenericsChan<T>> consumers, List<Holder<T>> producers, long timeout, boolean nonBlocking){
        long timeoutDeadLine =0L;
        if(timeout!=0L){
            timeoutDeadLine = System.currentTimeMillis()+timeout;
        }

        WishGroup<T> group = new WishGroup<>();
        for(GenericsChan<T> chan:consumers){
            new Wish<>(group,WISH_CONSUME,chan,null);
        }
        for(Holder<T> h:producers){
            new Wish<>(group,WISH_PRODUCE,h.chan,h.value);
        }
        Collections.shuffle(group.wishes);

        Set<ReentrantLock> set = new HashSet<>();
        for(Wish<T> wish:group.wishes){
            set.add(wish.chan.lock);
        }
        List<ReentrantLock> chanLockOrdered = new ArrayList<>(set);
        for(ReentrantLock r:chanLockOrdered){
            r.lock();
        }
        try {

            for(Wish<T> wish:group.wishes){
                /*//原来的
                if(wish.chan.closed){
                    throw new RuntimeException("closed");
                }
                */

                /*
                if(wish.chan.closed){
                    if(wish.kind==WISH_CONSUME){
                        return new Holder<>(wish.chan,null);
                    }else{
                        throw new RuntimeException("closed");
                    }
                }
                */

                if(wish.kind==WISH_CONSUME){
                    try {
                        T value = wish.chan.getNoWait();
                        return new Holder<>(wish.chan,value);
                    }catch (Exception ex){

                    }

                    //自己加的
                    if(wish.chan.closed){
                        //return null;//channel 关闭后且没值返回null
                        return new Holder<>(wish.chan,null);
                    }

                }else{

                    //自己加的
                    if(wish.chan.closed){
                        throw new RuntimeException("closed");
                    }

                    try{
                        wish.chan.putNoWait(wish.value);
                        return new Holder<>(wish.chan,null);
                    }catch (Exception ex){

                    }
                }
            }

            //A


            if(timeout!=0L && timeout<=0L){
                throw new RuntimeException("timeout");
            }

            if(nonBlocking){
                return null;
            }

            //A
            //两处A选择一处 加入一个 return null;

            for(Wish<T> wish:group.wishes){
                if(wish.kind==WISH_CONSUME){
                    wish.chan.waitingConsumers.addLast(wish);
                }else{
                    wish.chan.waitingProducers.addLast(wish);
                }
            }

        }finally {
            for(ReentrantLock r:chanLockOrdered){
                r.unlock();
            }
        }

        group.reentrantLock.lock();
        try {

            while (!group.fullFilled()){
                if(timeout == 0L){
                    try {
                        group.condition.await();
                    }catch (InterruptedException ex){
                        ex.printStackTrace();
                    }
                }else{
                    try {
                        group.condition.await(timeoutDeadLine-System.currentTimeMillis(),TimeUnit.MILLISECONDS);
                    }catch (InterruptedException ex){
                        ex.printStackTrace();
                    }
                    if(System.currentTimeMillis()>=timeoutDeadLine){
                        break;
                    }
                }
            }

        }finally {
            group.reentrantLock.unlock();
        }

        for(ReentrantLock r:chanLockOrdered){
            r.lock();
        }
        try {

            for(Wish<T> wish:group.wishes){
                if(wish.kind==WISH_CONSUME){
                    wish.chan.waitingConsumers.remove(wish);//原文这里会丢异常
                }else{
                    wish.chan.waitingProducers.remove(wish);//原文这里会丢异常
                }
            }

        }finally {
            for(ReentrantLock r:chanLockOrdered){
                r.unlock();
            }
        }

        Wish<T> wish = group.fullFilledBy;
        if(wish==null){
            throw new RuntimeException("timeout");
        }
        /*//原来是这样
        if(wish.closed){
            throw new RuntimeException("closed");
        }
        */

        if(wish.closed){
            if(wish.kind==WISH_CONSUME){

            }else{
                throw new RuntimeException("closed");
            }
        }
        //System.out.println(wish.value);
        return new Holder<T>(wish.chan,wish.value);

    }
}

//https://github.com/stuglaser/pychan/blob/master/chan/chan.py