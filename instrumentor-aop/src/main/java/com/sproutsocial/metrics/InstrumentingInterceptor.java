package com.sproutsocial.metrics;

import static com.sproutsocial.metrics.Instrumentation.errorGaugesExist;
import static com.sproutsocial.metrics.Instrumentation.registerErrorGauges;
import static com.sproutsocial.metrics.Instrumentation.registerHealthCheck;
import static com.sproutsocial.metrics.Instrumentation.shouldRegisterHealthCheck;
import static com.sproutsocial.metrics.Names.name;


import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Predicate;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.base.Strings;
import com.google.inject.Inject;

/**
 * Created on 4/17/15
 *
 * @author horthy
 */
public class InstrumentingInterceptor implements MethodInterceptor {
    

    private final MetricRegistry metricRegistry;
    private final HealthCheckRegistry healthCheckRegistry;
    private final Predicate<Throwable> filter;

    @Inject
    public InstrumentingInterceptor(
            MetricRegistry metricRegistry,
            HealthCheckRegistry healthCheckRegistry,
            Predicate<Throwable> filter
    ) {
        this.metricRegistry = metricRegistry;
        this.healthCheckRegistry = healthCheckRegistry;
        this.filter = filter;
    }


    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        final Method method = methodInvocation.getMethod();
        final Optional<Double> threshold = getErrorThreshold(method);
        final String name = getName(method);


        final Meter errorMeter = metricRegistry.meter(name + ".errors");
        final Timer timer = metricRegistry.timer(name);
        final Counter inFlight = metricRegistry.counter(name + ".inFlight");

        if (!errorGaugesExist(metricRegistry, name)) {
            registerErrorGauges(metricRegistry, name, errorMeter, timer);
        }

        if (shouldRegisterHealthCheck(healthCheckRegistry, name, threshold)) {
            registerHealthCheck(healthCheckRegistry, name, threshold, errorMeter, timer);
        }

        inFlight.inc();
        try (@SuppressWarnings("unused") Timer.Context ctx = timer.time()){
            return methodInvocation.proceed();
        } catch (Throwable e) {
            if (filter.test(e)) {
                errorMeter.mark();
            }
            throw e;
        } finally {
            inFlight.dec();
        }
    }

    private String getName(Method method) {
        final String name = method
                .getDeclaredAnnotation(Instrumented.class)
                .name();

        return Strings.isNullOrEmpty(name) ? name(method) : name;
    }

    private Optional<Double> getErrorThreshold(Method method) {
        final double threshold = method.getDeclaredAnnotation(Instrumented.class)
                .errorThreshold();
        return threshold == Instrumentation.NO_THRESHOLD_DEFINED ?
                Optional.empty() :
                Optional.of(threshold);
    }


}
