package concutrrent.locks;

import sun.misc.Unsafe;

public abstract class AbstractQueuedSynchronizer
    extends AbstractOwnableSynchronizer
    implements java.io.Serializable{
    private static final long serialVersionUID = -5337446129908268581L;

    protected AbstractQueuedSynchronizer() { }

    static final class Node {
        static final Node SHARED = new Node();
        static final Node EXCLUSIVE = null;
        static final int CANCELLED = 1;
        static final int SIGNAL = -1;
        static final int CONDITION = -2;
        static final int PROPAGATE = -3;

        volatile int waitStatus;

        volatile Node prev;

        volatile Node next;

        volatile Thread thread;

        Node nextWaiter;

        final boolean isShared() {
            return nextWaiter == SHARED;
        }

        final Node predecessor() throws NullPointerException{
            Node p = prev;
            if (p == null)
                throw new NullPointerException();
            else
                return p;
        }

        Node() {
        }

        Node(Thread thread, Node node) {
            this.nextWaiter = mode;
            this.thread = thread;
        }

        Node(Thread thread, int waitStatus) {
            this.waitStatus = waitStatus;
            this.thread = thread;
        }
    }


    private transient volatile Node head;

    private transient volatile Node tail;

    private volatile int state;

    protected final int getState() {
        return state;
    }

    protected final void setState(int newState) {
        state = newState;
    }

    protected final boolean compareAndSetState(int expect, int update) {
        return unsafe.compareAndSwapInt(this, stateoffset, expect, update);
    }

    static final long spinForTimeoutThreshold = 1000l;

    /**
     * 设置节点
     * @param node
     * @return
     */
    private Node enq(final Node node) {
        for(;;) {
            Node t = tail;
            if(t == null) {
                if(compareAndSetHead(new Node()))
                    tail = head;
            } else {
                node.prev = t;
                if(compareAndSetTail(t, node)) {
                    t.next = node;
                    return t;
                }
            }
        }
    }

    /**
     * addWaiter方法和enq的第一个else部分是很像，但仔细看最后的return是不同的。
     * @param mode
     * @return
     */
    private Node addWaiter(Node mode) {
        // 初始化节点，设置关联线程和模式(独占 or 共享)
        Node node = new Node(Thread.currentThread(), mode);
        // 获取尾节点引用
        Node pred = tail;
        // 尾节点不为空，说明队列已经初始化过
        if(pred != null) {
            node.prev = pred;
            // 设置新节点为尾节点
            if(compareAndSetTail(pred, node)) {
                pred.next = mode;
                return node;
            }
        }
        // 尾节点为空， 说明队列还未初始化，需要初始化head节点并入队新节点
        enq(node);
        return node;

    }


    private void setHead(Node node) {
        head = node;
        node.thread = null;
        node.prev = null;
    }


    final boolean acquireQueued(final Node node, int arg) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for(;;) {
                final Node p = node.predecessor();
                if(p == head && tryAcquire(arg)) {
                    setHead(node);
                    p.next = null;
                    failed = false;
                    return interrupted;
                }
                if(shouldParkAfterFailedAcquire(p, node) &&
                        packAndCheckInterrupt())
                    interrupted = true;
            }
        } finally {
            if (failed)
                cancelAcquire(node)
        }
    }


    public final void acquire(int arg) {
        if(!tryAcquire(arg) &&
            acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
            selfInterrupt();
    }

    /**
     * 尝试在独占模式下获取。此方法应该查询
     * 如果对象的状态允许在
     * 排他模式，如果是，获取它。
     *
     * 该方法总是由执行的线程调用
     * 获取。如果此方法报告失败，则使用获取方法
     * 可以让线程排队，如果它还没有排队，直到它排队
     * 从其他线程释放信号。这可以用
     * 实现方法{@link Lock#tryLock()}。
     * @param arg
     * @return
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }





    private static final Unsafe unsafe = Unsafe.getUnsafe();
    private static final long stateoffset;
    private static final long headoffset;
    private static final long tailoffset;
    private static final long waitStatusOffset;
    private static final long nextOffset;

    static {
        try {
            stateoffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("state"));
            headoffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("head"));
            tailoffset = unsafe.objectFieldOffset
                    (AbstractQueuedSynchronizer.class.getDeclaredField("tail"));
            waitStatusOffset = unsafe.objectFieldOffset
                    (Node.class.getDeclaredField("waitStatus"));
            nextOffset = unsafe.objectFieldOffset
                    (Node.class.getDeclaredField("next"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    /**
     * 设置头结点
     * @param update
     * @return
     */
    private final boolean compareAndSetHead(Node update) {
        return unsafe.compareAndSwapObject(this, headoffset, null, update);
    }

    /***
     * 设置尾节点
     * @param expect
     * @param update
     * @return
     */
    private final boolean compareAndSetTail(Node expect, Node update) {
        return unsafe.compareAndSwapObject(this, tailoffset, expect, update);
    }

    private static final boolean compareAndSetWaitStatus(Node node,
                                                         int expect,
                                                         int update) {
        return unsafe.compareAndSwapInt(node, waitStatusOffset,
                                        expect, update);
    }
}
