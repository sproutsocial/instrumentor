package com.sproutsocial.metrics;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


import java.sql.SQLException;

import org.junit.Test;

/**
 * Created on 3/17/16.
 */
public class ExceptionFiltersTest {
    @Test
    public void testMarkAll() throws Exception {
        assertTrue(ExceptionFilters.markAllExceptions().test(new Throwable()));
        assertTrue(ExceptionFilters.markAllExceptions().test(new RuntimeException()));
        assertTrue(ExceptionFilters.markAllExceptions().test(new NullPointerException()));
        assertTrue(ExceptionFilters.markAllExceptions().test(new IllegalArgumentException()));

        assertTrue(ExceptionFilters.markAllExceptions().test(new Exception()));
        assertTrue(ExceptionFilters.markAllExceptions().test(new SQLException()));
    }

    @Test
    public void testMarkChecked() throws Exception {
        assertFalse(ExceptionFilters.markCheckedExceptions().test(new Throwable()));
        assertFalse(ExceptionFilters.markCheckedExceptions().test(new RuntimeException()));
        assertFalse(ExceptionFilters.markCheckedExceptions().test(new NullPointerException()));
        assertFalse(ExceptionFilters.markCheckedExceptions().test(new IllegalArgumentException()));

        assertTrue(ExceptionFilters.markCheckedExceptions().test(new Exception()));
        assertTrue(ExceptionFilters.markCheckedExceptions().test(new SQLException()));
    }
}
