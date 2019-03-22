package com.sproutsocial.metrics;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
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
import com.google.inject.Injector;

/**
 * Created on 4/18/15
 *
 * @author horthy
 */
@RunWith(MockitoJUnitRunner.class)
public class InstrumentingInterceptorTest {


    public static final String NAME_METHOD = "method";
    public static final String NAME_CLASS = "class";
    public static final String ANOTHER_NAME_CLASS = "anotherclass";
    private @Mock Meter errorMeter;
    private @Mock Timer timer;
    private @Mock Counter counter;
    private @Mock Timer.Context context;

    private @Mock MetricRegistry metricRegistry;
    private @Mock HealthCheckRegistry healthCheckRegistry;

    private MethodAnnotatedTestStub methodTestStub;
    private ClassAnnotatedTestStub classTestStub;


    public static class MethodAnnotatedTestStub {
        @Instrumented(name = NAME_METHOD, errorThreshold = 0.5d)
        public void faultyMethod() {
            throw new RuntimeException();
        }
    }

    public static interface NonAnnotatedInterfaceWithDefaultMethod {
        default String successfulMethod() {
            return "arbitrary data to verify method ran";
        }
    }

    @Instrumented(name = ANOTHER_NAME_CLASS, errorThreshold = 0.5d)
    public static interface AnnotatedInterfaceWithDefaultMethod {
        default void anotherFaultyMethod() {
          throw new RuntimeException();
        }
    }

    @Instrumented(name = NAME_CLASS, errorThreshold = 0.5d)
    public static class ClassAnnotatedTestStub implements
        NonAnnotatedInterfaceWithDefaultMethod, AnnotatedInterfaceWithDefaultMethod {
        public void faultyMethod() {
            throw new RuntimeException();
        }
    }

    @Before
    public void setUp() throws Exception {
        final InstrumentedAnnotations instrumentedAnnotations =
                InstrumentedAnnotations.builder()
                        .metricRegistry(metricRegistry)
                        .healthCheckRegistry(healthCheckRegistry)
                        .build();

        Injector injector = Guice.createInjector(instrumentedAnnotations);

        methodTestStub = injector.getInstance(MethodAnnotatedTestStub.class);
        classTestStub = injector.getInstance(ClassAnnotatedTestStub.class);
    }

    @Test
    public void testTimerStopsOnceOnException() throws Exception {
        when(metricRegistry.timer(NAME_METHOD)).thenReturn(timer);
        when(metricRegistry.meter(NAME_METHOD + ".errors")).thenReturn(errorMeter);
        when(metricRegistry.counter(NAME_METHOD + ".inFlight")).thenReturn(counter);

        when(timer.time()).thenReturn(context);

        try {
            methodTestStub.faultyMethod();
        } catch (RuntimeException ignored) {}

        verify(healthCheckRegistry, times(1)).register(eq(NAME_METHOD), any(HealthCheck.class));

        InOrder inOrder = inOrder(context, errorMeter, counter);
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).dec();

    }

    @Test
    public void testClassAnnotation() throws Exception {
        String methodName = NAME_CLASS + ".faultyMethod";

        when(metricRegistry.timer(methodName)).thenReturn(timer);
        when(metricRegistry.meter(methodName + ".errors")).thenReturn(errorMeter);
        when(metricRegistry.counter(methodName + ".inFlight")).thenReturn(counter);

        when(timer.time()).thenReturn(context);

        try {
            classTestStub.faultyMethod();
        } catch (RuntimeException ignored) {}

        verify(healthCheckRegistry, times(1)).register(eq(methodName), any(HealthCheck.class));

        InOrder inOrder = inOrder(context, errorMeter, counter);
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).dec();

    }

    @Test
    public void testNonAnnotatedInterfaceDefaultMethod() {
        assertEquals(
            "arbitrary data to verify method ran",
            classTestStub.successfulMethod()
        );
        verifyZeroInteractions(
            context,
            errorMeter,
            counter,
            timer,
            healthCheckRegistry,
            metricRegistry
        );
    }

    @Test
    public void testAnnotatedInterfaceDefaultMethod() throws Exception {
        String methodName = ANOTHER_NAME_CLASS + ".anotherFaultyMethod";

        when(metricRegistry.timer(methodName)).thenReturn(timer);
        when(metricRegistry.meter(methodName + ".errors")).thenReturn(errorMeter);
        when(metricRegistry.counter(methodName + ".inFlight")).thenReturn(counter);

        when(timer.time()).thenReturn(context);

        try {
            classTestStub.anotherFaultyMethod();
        } catch (RuntimeException ignored) {}

        verify(healthCheckRegistry, times(1)).register(eq(methodName), any(HealthCheck.class));

        InOrder inOrder = inOrder(context, errorMeter, counter);
        inOrder.verify(counter, times(1)).inc();
        inOrder.verify(context, times(1)).close();
        inOrder.verify(errorMeter, times(1)).mark();
        inOrder.verify(counter, times(1)).dec();
    }

}
