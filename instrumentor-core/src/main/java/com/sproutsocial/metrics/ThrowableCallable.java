package com.sproutsocial.metrics;

/**
 * Created on 1/20/16
 */
public interface ThrowableCallable<T> {
    T call() throws Throwable;
}
