package com.jaoow.sql.executor;

public class ThreadLocalTransaction {
    private static final ThreadLocal<SQLTransactionHolder> THREAD_LOCAL_TRANSACTION = new ThreadLocal<>();


    /**
     * This method is used to get the SQLTransactionHolder associated with the current thread.
     * If there is no SQLTransactionHolder associated with the current thread, it returns null.
     *
     * @return the SQLTransactionHolder associated with the current thread, or null if there is none
     */
    public static SQLTransactionHolder get() {
        return THREAD_LOCAL_TRANSACTION.get();
    }

    /**
     * This method is used to set the SQLTransactionHolder associated with the current thread.
     * If there is already a SQLTransactionHolder associated with the current thread, it is replaced.
     *
     * @param transactionHolder the SQLTransactionHolder to be associated with the current thread
     */
    public static void set(SQLTransactionHolder transactionHolder) {
        THREAD_LOCAL_TRANSACTION.set(transactionHolder);
    }

    /**
     * This method is used to remove the SQLTransactionHolder associated with the current thread.
     * If there is no SQLTransactionHolder associated with the current thread, this method does nothing.
     */
    public static void remove() {
        THREAD_LOCAL_TRANSACTION.remove();
    }
}
