package com.sproutsocial.metrics;

import java.util.function.Predicate;

/**
 * Created on 3/17/16.
 */
public final class ExceptionFilters {

    private ExceptionFilters() {}

    public static Predicate<Throwable> markAllExceptions() {
        return new AnyThrowable();
    }

    public static Predicate<Throwable> markCheckedExceptions() {
        return new CheckedExceptions();
    }

    private static class AnyThrowable implements Predicate<Throwable> {
        @Override
        public boolean test(Throwable throwable) {
            return true;
        }
    }

    private static class CheckedExceptions implements Predicate<Throwable> {
        @Override
        public boolean test(Throwable throwable) {
            return throwable instanceof Exception && !(throwable instanceof RuntimeException);
        }
    }
}
