package com.sproutsocial.metrics;

/**
 * Created on 1/20/16
 */
/* package */ interface ThrowableCallable<T> {
    T call() throws Throwable;
}
