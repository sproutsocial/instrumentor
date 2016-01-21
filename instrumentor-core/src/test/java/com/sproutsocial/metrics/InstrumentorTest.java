package com.sproutsocial.metrics;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;

/**
 * Created on 4/18/15
 *
 * @author horthy
 */
@RunWith(MockitoJUnitRunner.class)
public class InstrumentorTest {

    public static final String NAME = "method";
    private @Mock Meter errorMeter;
    private @Mock Timer timer;
    private @Mock Counter counter;
    private @Mock Timer.Context context;
    private @Mock MetricRegistry metricRegistry;
    private HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();

    private Instrumentor instrumentor;

    @Before
    public void setUp() throws Exception {
        instrumentor = Instrumentor.builder()
                .metricRegistry(metricRegistry)
                .healthCheckRegistry(healthCheckRegistry)
                .build();
    }

    @Test
    public void testRunnable() throws Exception {
        when(metricRegistry.timer(NAME)).thenReturn(timer);
        when(metricRegistry.meter(NAME + ".errors")).thenReturn(errorMeter);
        when(metricRegistry.counter(NAME + ".inFlight")).thenReturn(counter);
        when(timer.time()).thenReturn(context);
        Runnable runnable =  () -> { throw new RuntimeException();};

        try {
            instrumentor.instrumenting(runnable, NAME, 0.1).run();
        } catch (RuntimeException ignored) {}

        assertTrue(healthCheckRegistry.getNames().contains(NAME));

        InOrder inOrder = inOrder(errorMeter, context, counter);


        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).dec();

    }

    @Test
    public void testCheckedRunnable() throws Exception {
        when(metricRegistry.timer(NAME)).thenReturn(timer);
        when(metricRegistry.meter(NAME + ".errors")).thenReturn(errorMeter);
        when(metricRegistry.counter(NAME + ".inFlight")).thenReturn(counter);
        when(timer.time()).thenReturn(context);
        CheckedRunnable runnable =  () -> { throw new Exception();};


        try {
            instrumentor.runChecked(runnable, NAME, 0.1);
        } catch (Exception ignored) {}

        assertTrue(healthCheckRegistry.getNames().contains(NAME));


        final InOrder inOrder = inOrder(counter, errorMeter, context);
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).dec();
    }

    @Test
    public void testCallable() throws Exception {
        when(metricRegistry.timer(NAME)).thenReturn(timer);
        when(metricRegistry.meter(NAME + ".errors")).thenReturn(errorMeter);
        when(metricRegistry.counter(NAME + ".inFlight")).thenReturn(counter);
        when(timer.time()).thenReturn(context);
        Callable<Void> callable =  () -> { throw new Exception();};


        try {
            instrumentor.call(callable, NAME, 0.1);
        } catch (Exception ignored) {}

        assertTrue(healthCheckRegistry.getNames().contains(NAME));


        final InOrder inOrder = inOrder(counter, errorMeter, context);
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).dec();
    }

    @Test
    public void testCheckedCallable() throws Exception {
        when(metricRegistry.timer(NAME)).thenReturn(timer);
        when(metricRegistry.meter(NAME + ".errors")).thenReturn(errorMeter);
        when(metricRegistry.counter(NAME + ".inFlight")).thenReturn(counter);
        when(timer.time()).thenReturn(context);
        Callable<Void> callable =  () -> { throw new Exception();};


        try {
            instrumentor.callChecked(callable, NAME, 0.1);
        } catch (Exception ignored) {}

        assertTrue(healthCheckRegistry.getNames().contains(NAME));


        final InOrder inOrder = inOrder(counter, errorMeter, context);
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).dec();
    }

    @Test
    public void testCallThrowably() throws Exception {
        when(metricRegistry.timer(NAME)).thenReturn(timer);
        when(metricRegistry.meter(NAME + ".errors")).thenReturn(errorMeter);
        when(metricRegistry.counter(NAME + ".inFlight")).thenReturn(counter);
        when(timer.time()).thenReturn(context);
        ThrowableCallable<Void> callable =  () -> { throw new Throwable();};


        try {
            instrumentor.callThrowably(callable, NAME, 0.1);
        } catch (Throwable ignored) {}

        assertTrue(healthCheckRegistry.getNames().contains(NAME));


        final InOrder inOrder = inOrder(counter, errorMeter, context);
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).dec();
    }
}
