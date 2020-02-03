package com.ywcjxf.java.go.concurrent.chan.impl.third.pychan;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Chan{
    private static class WishGroup{
        private Wish fullFilledBy = null;
        private ReentrantLock reentrantLock = new ReentrantLock();
        private Condition condition = reentrantLock.newCondition();
        private List<Wish> wishes = new ArrayList<>(10);

        public boolean fullFilled(){
            return fullFilledBy!=null;
        }
    }

    private static int WISH_PRODUCE =0;
    private static int WISH_CONSUME = 1;

    private static class Wish{
        private WishGroup group;
        private int kind;
        private Chan chan;
        private Object value;
        private boolean closed = false;

        public Wish(WishGroup group, int kind, Chan chan, Object value) {//Object value 可省略
            this.group = group;
            this.kind = kind;
            this.chan = chan;
            this.value = value;

            this.group.wishes.add(this);
        }

        public boolean fullFilled(){
            return group.fullFilled();
        }

        public Object fullFill(Object value,boolean closed){//Object value 可省略 boolean closed可省略
            assert !fullFilled();
            this.closed=closed;
            group.fullFilledBy = this;
            group.condition.signal();//唤醒
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

    private static class RingBuffer{
        private Object[] buf;
        private int nextPop = 0;
        private int len = 0;

        public RingBuffer(int bufLen) {
            this.buf = new Object[bufLen];
        }

        public int cap(){
            return buf.length;
        }

        public void push(Object value){
            if(len==buf.length){
                throw new ArrayIndexOutOfBoundsException();
            }
            int nextPush = (this.nextPop+len)%buf.length;
            buf[nextPush] = value;
            //System.out.println("push "+value);
            len+=1;
        }

        public Object pop(){
            if(len==0){
                throw new ArrayIndexOutOfBoundsException();
            }
            Object value = buf[nextPop];
            buf[nextPop] = null;
            nextPop = (nextPop+1)%buf.length;
            len-=1;
            //System.out.println("pop "+value);
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
    private RingBuffer buf;
    private Deque<Wish> waitingProducers = new LinkedList<>();
    private Deque<Wish> waitingConsumers = new LinkedList<>();

    public Chan() {
        this(0);
    }

    public Chan(int bufLen) {//int bufLen默认值
        if(bufLen>0){
            buf = new RingBuffer(bufLen);
        }
    }

    private Object fullFillWaitingProducer(){
        while (true){
            if(!waitingProducers.isEmpty()){
                Wish productWish = waitingProducers.removeFirst();
                productWish.group.reentrantLock.lock();
                try {
                    if(!productWish.group.fullFilled()){
                        return productWish.fullFill(null,false);//默认值 在原来的代码里是使用默认值
                    }
                }finally {
                    productWish.group.reentrantLock.unlock();
                }
            }else{
                throw new RuntimeException("empty");
            }
        }
    }

    public Object getNoWait(){//这个方法难理解
        if(buf!=null && !buf.empty()){//作用是buffer没空 从buffer中取一个之后 从等待的生产者那里取一个值放到buffer里
            Object value = buf.pop();
            try {
                Object produced = fullFillWaitingProducer();
                //System.out.println(produced+"pro"+" "+value);
                buf.push(produced);
            }catch (Exception ex){
                //System.out.println("except");
            }
            return value;
        }else{
            return fullFillWaitingProducer();
        }
    }

    public void putNoWait(Object value){
        while (true){
            if(!waitingConsumers.isEmpty()){
                Wish consumeWish = waitingConsumers.removeFirst();
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

    public Object get(){
        return get(0);
    }

    public Object get(long timeout){
        long timeoutDeadLine =0L;
        if(timeout!=0L){
            timeoutDeadLine = System.currentTimeMillis()+timeout;
        }

        WishGroup group;
        Wish wish;
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

            group = new WishGroup();
            wish = new Wish(group,WISH_CONSUME,this,null);
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

            /*
            if(wish.closed){
                return null;//?
                //throw new RuntimeException("closed");
            }
            */
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

    public void put(Object value){
        put(value,0);
    }

    public void put(Object value,long timeout){
        Objects.requireNonNull(value);//加了这一行 保证channel不能放null

        long timeoutDeadLine =0L;
        if(timeout!=0L){
            timeoutDeadLine = System.currentTimeMillis()+timeout;
        }

        WishGroup group;
        Wish wish;
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

            group = new WishGroup();
            wish = new Wish(group,WISH_PRODUCE,this,value);
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

            /*
            if(wish.closed){
                throw new RuntimeException("closed");
            }
            */
        }finally {
            group.reentrantLock.unlock();
        }

        //原来放在这里
        if(wish.closed){
            throw new RuntimeException("closed");
        }

    }

    public void close(){
        Deque<Wish> wishes = new LinkedList<>();
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

        for(Wish wish:wishes){
            wish.group.reentrantLock.lock();
            try {
                if(!wish.fullFilled()){
                    wish.fullFill(null,true);//这个null会把交给消费者的值变成null
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

    public static class Holder{
        public Chan chan;
        public Object value;

        public Holder(Chan chan, Object value) {
            this.chan = chan;
            this.value = value;
        }
    }

    public static Holder chanSelect(List<Chan> consumers, List<Holder> producers){
        return chanSelect(consumers,producers,0);
    }

    public static Holder chanSelect(List<Chan> consumers, List<Holder> producers, long timeout){
        return tryChanSelect(consumers,producers,timeout,false);
    }

    public static Holder tryChanSelect(List<Chan> consumers, List<Holder> producers){
        return tryChanSelect(consumers,producers,0,true);
    }

    public static Holder tryChanSelect(List<Chan> consumers, List<Holder> producers, long timeout, boolean nonBlocking){
        long timeoutDeadLine =0L;
        if(timeout!=0L){
            timeoutDeadLine = System.currentTimeMillis()+timeout;
        }

        WishGroup group = new WishGroup();
        for(Chan chan:consumers){
            new Wish(group,WISH_CONSUME,chan,null);
        }
        for(Holder h:producers){
            new Wish(group,WISH_PRODUCE,h.chan,h.value);
        }
        Collections.shuffle(group.wishes);

        Set<ReentrantLock> set = new HashSet<>();
        for(Wish wish:group.wishes){
            set.add(wish.chan.lock);
        }
        List<ReentrantLock> chanLockOrdered = new ArrayList<>(set);
        for(ReentrantLock r:chanLockOrdered){
            r.lock();
        }
        try {

            for(Wish wish:group.wishes){
                /*//原来的
                if(wish.chan.closed){
                    throw new RuntimeException("closed");
                }
                */

                /*//自己改的
                if(wish.chan.closed){
                    if(wish.kind==WISH_CONSUME){
                        return new Holder(wish.chan,null);
                    }else{
                        throw new RuntimeException("closed");
                    }
                }
                */


                if(wish.kind==WISH_CONSUME){
                    try {
                        Object value = wish.chan.getNoWait();
                        return new Holder(wish.chan,value);
                    }catch (Exception ex){

                    }

                    //自己加的
                    if(wish.chan.closed){
                        //return null;//channel 关闭后且没值返回null
                        return new Holder(wish.chan,null);
                    }

                }else{

                    //自己加的
                    if(wish.chan.closed){
                        throw new RuntimeException("closed");
                    }

                    try{
                        wish.chan.putNoWait(wish.value);
                        return new Holder(wish.chan,null);
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

            for(Wish wish:group.wishes){
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

            for(Wish wish:group.wishes){
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

        Wish wish = group.fullFilledBy;
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
        return new Holder(wish.chan,wish.value);

    }
}

//https://github.com/stuglaser/pychan/blob/master/chan/chan.py