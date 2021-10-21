package com.sproutsocial.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExtendedInstrumentorTest {

    public static final String NAME = "method";
    private @Mock Meter totalMeter;
    private @Mock Meter successMeter;
    private @Mock Meter errorMeter;
    private @Mock Timer timer;
    private @Mock Counter counter;
    private @Mock Timer.Context context;
    private @Mock MetricRegistry metricRegistry;
    private final HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();

    private ExtendedInstrumentor instrumentor;

    @Before
    public void setUp() {
        instrumentor = new ExtendedInstrumentor(metricRegistry, healthCheckRegistry, any -> true);
    }

    @Test
    public void testRunnable_error() {
        initializeMetrics();
        Runnable runnable = () -> {
            throw new RuntimeException();
        };

        try {
            instrumentor.instrumenting(runnable, NAME, 0.1).run();
        } catch (RuntimeException ignored) {
        }

        assertTrue(healthCheckRegistry.getNames().contains(NAME));

        InOrder inOrder = inOrder(totalMeter, successMeter, errorMeter, context, counter);
        inOrder.verify(totalMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(successMeter, times(0)).mark();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).dec();
    }

    @Test
    public void testRunnable_success() {
        initializeMetrics();
        Runnable runnable = () -> {
        };

        try {
            instrumentor.instrumenting(runnable, NAME, 0.1).run();
        } catch (RuntimeException ignored) {
        }

        assertTrue(healthCheckRegistry.getNames().contains(NAME));

        InOrder inOrder = inOrder(totalMeter, successMeter, errorMeter, context, counter);
        inOrder.verify(totalMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(successMeter, times(1)).mark();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(0)).mark();
        inOrder.verify(counter, times(1)).dec();
    }

    @Test
    public void testCheckedRunnable_error() {
        initializeMetrics();
        CheckedRunnable runnable = () -> {
            throw new Exception();
        };


        try {
            instrumentor.runChecked(runnable, NAME, 0.1);
        } catch (Exception ignored) {
        }

        assertTrue(healthCheckRegistry.getNames().contains(NAME));


        final InOrder inOrder = inOrder(totalMeter, successMeter, counter, errorMeter, context);
        inOrder.verify(totalMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(successMeter, times(0)).mark();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).dec();
    }

    @Test
    public void testCheckedRunnable_success() {
        initializeMetrics();
        CheckedRunnable runnable = () -> {
        };


        try {
            instrumentor.runChecked(runnable, NAME, 0.1);
        } catch (Exception ignored) {
        }

        assertTrue(healthCheckRegistry.getNames().contains(NAME));


        InOrder inOrder = inOrder(totalMeter, successMeter, errorMeter, context, counter);
        inOrder.verify(totalMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(successMeter, times(1)).mark();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(0)).mark();
        inOrder.verify(counter, times(1)).dec();
    }

    @Test
    public void testCallable_error() {
        initializeMetrics();
        Callable<Void> callable = () -> {
            throw new Exception();
        };


        try {
            instrumentor.call(callable, NAME, 0.1);
        } catch (Exception ignored) {
        }

        assertTrue(healthCheckRegistry.getNames().contains(NAME));


        InOrder inOrder = inOrder(totalMeter, successMeter, errorMeter, context, counter);
        inOrder.verify(totalMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(successMeter, times(0)).mark();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).dec();
    }

    @Test
    public void testCallable_success() {
        initializeMetrics();
        Callable<Void> callable = () -> null;


        try {
            instrumentor.call(callable, NAME, 0.1);
        } catch (Exception ignored) {
        }

        assertTrue(healthCheckRegistry.getNames().contains(NAME));


        InOrder inOrder = inOrder(totalMeter, successMeter, errorMeter, context, counter);
        inOrder.verify(totalMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(successMeter, times(1)).mark();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(0)).mark();
        inOrder.verify(counter, times(1)).dec();
    }

    @Test
    public void testCheckedCallable_error() {
        initializeMetrics();
        Callable<Void> callable = () -> {
            throw new Exception();
        };


        try {
            instrumentor.callChecked(callable, NAME, 0.1);
        } catch (Exception ignored) {
        }

        assertTrue(healthCheckRegistry.getNames().contains(NAME));


        InOrder inOrder = inOrder(totalMeter, successMeter, errorMeter, context, counter);
        inOrder.verify(totalMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(successMeter, times(0)).mark();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).dec();
    }

    @Test
    public void testCheckedCallable_success() {
        initializeMetrics();
        Callable<Void> callable = () -> null;


        try {
            instrumentor.callChecked(callable, NAME, 0.1);
        } catch (Exception ignored) {
        }

        assertTrue(healthCheckRegistry.getNames().contains(NAME));


        InOrder inOrder = inOrder(totalMeter, successMeter, errorMeter, context, counter);
        inOrder.verify(totalMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(successMeter, times(1)).mark();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(0)).mark();
        inOrder.verify(counter, times(1)).dec();
    }

    @Test
    public void testCallThrowably_error() {
        initializeMetrics();
        ThrowableCallable<Void> callable = () -> {
            throw new Throwable();
        };


        try {
            instrumentor.callThrowably(callable, NAME, 0.1);
        } catch (Throwable ignored) {
        }

        assertTrue(healthCheckRegistry.getNames().contains(NAME));


        InOrder inOrder = inOrder(totalMeter, successMeter, errorMeter, context, counter);
        inOrder.verify(totalMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(successMeter, times(0)).mark();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).dec();
    }

    @Test
    public void testCallThrowably_success() {
        initializeMetrics();
        ThrowableCallable<Void> callable = () -> null;


        try {
            instrumentor.callThrowably(callable, NAME, 0.1);
        } catch (Throwable ignored) {
        }

        assertTrue(healthCheckRegistry.getNames().contains(NAME));


        InOrder inOrder = inOrder(totalMeter, successMeter, errorMeter, context, counter);
        inOrder.verify(totalMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(successMeter, times(1)).mark();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(0)).mark();
        inOrder.verify(counter, times(1)).dec();
    }

    private void initializeMetrics() {
        when(metricRegistry.timer(NAME)).thenReturn(timer);
        when(metricRegistry.meter(NAME + ".total")).thenReturn(totalMeter);
        when(metricRegistry.meter(NAME + ".success")).thenReturn(successMeter);
        when(metricRegistry.meter(NAME + ".errors")).thenReturn(errorMeter);
        when(metricRegistry.counter(NAME + ".inFlight")).thenReturn(counter);
        when(timer.time()).thenReturn(context);
    }
}
