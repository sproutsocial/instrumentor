package com.sproutsocial.metrics;

/**
 * Created on 11/9/15
 *
 * Runnable that can throw an exception
 */
@FunctionalInterface
public interface CheckedRunnable<T extends Exception> {
    void run() throws T;
}
