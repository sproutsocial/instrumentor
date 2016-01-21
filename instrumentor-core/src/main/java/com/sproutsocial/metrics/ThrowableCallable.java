package com.sproutsocial.metrics;

/**
 * Created on 1/20/16
 * a version of {@link java.util.concurrent.Callable}
 * that throws {@link Throwable}.
 *
 * Package scoped, used in AOP module
 */
/* package */ interface ThrowableCallable<T> {
    T call() throws Throwable;
}
