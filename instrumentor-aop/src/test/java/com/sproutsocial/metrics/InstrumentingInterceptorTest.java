package com.sproutsocial.metrics;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


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
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.Guice;

/**
 * Created on 4/18/15
 *
 * @author horthy
 */
@RunWith(MockitoJUnitRunner.class)
public class InstrumentingInterceptorTest {


    public static final String NAME = "method";
    private @Mock Meter errorMeter;
    private @Mock Timer timer;
    private @Mock Counter counter;
    private @Mock Timer.Context context;

    private @Mock MetricRegistry metricRegistry;
    private @Mock HealthCheckRegistry healtchCheckRegistry;

    private TestStub testStub;


    public static class TestStub {
        @Instrumented(name = NAME, errorThreshold = 0.5d)
        public void faultyMethod() {
            throw new RuntimeException();
        }
    }

    @Before
    public void setUp() throws Exception {
        final InstrumentedAnnotations instrumentedAnnotations =
                InstrumentedAnnotations.builder()
                        .metricRegistry(metricRegistry)
                        .healthCheckRegistry(healtchCheckRegistry)
                        .build();

        testStub = Guice.createInjector(instrumentedAnnotations)
                        .getInstance(TestStub.class);
    }

    @Test
    public void testTimerStopsOnceOnException() throws Exception {
        when(metricRegistry.timer(NAME)).thenReturn(timer);
        when(metricRegistry.meter(NAME + ".errors")).thenReturn(errorMeter);
        when(metricRegistry.counter(NAME + ".inFlight")).thenReturn(counter);

        when(timer.time()).thenReturn(context);
        
        try {
            testStub.faultyMethod();
        } catch (RuntimeException ignored) {}

        verify(healtchCheckRegistry, times(1)).register(eq(NAME), any(HealthCheck.class));

        InOrder inOrder = inOrder(context, errorMeter, counter);
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).dec();

    }
}
