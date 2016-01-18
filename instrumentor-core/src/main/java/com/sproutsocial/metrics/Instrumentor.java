package com.sproutsocial.metrics;

import static com.sproutsocial.metrics.Instrumentation.errorGaugesExist;
import static com.sproutsocial.metrics.Instrumentation.registerErrorGauges;
import static com.sproutsocial.metrics.Instrumentation.registerHealthCheck;
import static com.sproutsocial.metrics.Instrumentation.shouldRegisterHealthCheck;


import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.base.Throwables;

/**
 * Created on 4/17/15
 *
 * @author horthy
 */
public class Instrumentor {

    private final MetricRegistry metricRegistry;
    private final Optional<HealthCheckRegistry> healthCheckRegistry;
    private final Predicate<Throwable> filter;

    public Instrumentor() {
        this(new MetricRegistry(), new HealthCheckRegistry(), any -> true);
    }

    public Instrumentor(
            MetricRegistry metricRegistry
    ) {
        this(metricRegistry, null, any -> true);
    }

    public Instrumentor(
        MetricRegistry metricRegistry,
        HealthCheckRegistry healthCheckRegistry
    ) {
        this(metricRegistry, healthCheckRegistry, any -> true);
    }

    public Instrumentor(
            MetricRegistry metricRegistry,
            HealthCheckRegistry healthCheckRegistry,
            Predicate<Throwable> filter) {
        this.metricRegistry = metricRegistry;
        this.healthCheckRegistry = Optional.ofNullable(healthCheckRegistry);
        this.filter = filter;
    }

    /**
     * @return the underlying {@link HealthCheckRegistry}
     */
    public Optional<HealthCheckRegistry> getHealthCheckRegistry() {
        return healthCheckRegistry;
    }

    /**
     * @return the underlying {@link MetricRegistry}
     */
    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    public <T> Callable<T> instrumenting(
            Callable<T> callable,
            String name,
            Optional<Double> errorThreshold
    ) {

        final Meter errorMeter = metricRegistry.meter(name + ".errors");
        final Timer timer = metricRegistry.timer(name);
        final Counter inFlight = metricRegistry.counter(name + ".inFlight");

        registerMetricsAndHealthChecks(timer, errorMeter, name, errorThreshold);

        return () -> {
            inFlight.inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = timer.time()){
                return callable.call();
            } catch (Exception e) {
                if (filter.test(e)) {
                    errorMeter.mark();
                }
                throw e;
            } finally {
                inFlight.dec();
            }
        };
    }

    public CheckedRunnable instrumenting(
            CheckedRunnable runnable,
            String name,
            Optional<Double> errorThreshold
    ) {

        final Meter errorMeter = metricRegistry.meter(name + ".errors");
        final Timer timer = metricRegistry.timer(name);
        final Counter inFlight = metricRegistry.counter(name + ".inFlight");

        registerMetricsAndHealthChecks(timer, errorMeter, name, errorThreshold);

        return () -> {
            inFlight.inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = timer.time()){
                runnable.run();
            } catch (Exception e) {
                if (filter.test(e)) {
                    errorMeter.mark();
                }
                throw e;
            } finally {
                inFlight.dec();
            }
        };
    }

    public Runnable instrumenting(
            Runnable runnable,
            String name,
            Optional<Double> errorThreshold
    ) {

        final Meter errorMeter = metricRegistry.meter(name + ".errors");
        final Timer timer = metricRegistry.timer(name);
        final Counter inFlight = metricRegistry.counter(name + ".inFlight");

        registerMetricsAndHealthChecks(timer, errorMeter, name, errorThreshold);

        return () -> {
            inFlight.inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = timer.time()){
                runnable.run();
            } catch (Exception e) {
                if (filter.test(e)) {
                    errorMeter.mark();
                }
                throw e;
            } finally {
                inFlight.dec();
            }
        };
    }

    private void registerMetricsAndHealthChecks(Timer timer, Meter errorMeter, String name, Optional<Double> errorThreshold) {
        if (!errorGaugesExist(metricRegistry, name)) {
            registerErrorGauges(metricRegistry, name, errorMeter, timer);
        }

        if (shouldRegisterHealthCheck(healthCheckRegistry, name, errorThreshold)) {
            registerHealthCheck(healthCheckRegistry.get(), name, errorThreshold, errorMeter, timer);
        }
    }

    public void run(
            Runnable runnable,
            String name,
            Optional<Double> errorThreshold
    ) {
        instrumenting(runnable, name, errorThreshold).run();
    }

    public void runChecked(
            CheckedRunnable runnable,
            String name,
            Optional<Double> errorThreshold
    ) throws Exception {
        instrumenting(runnable, name, errorThreshold).run();
    }

    public <T> T call(
            Callable<T> callable,
            String name,
            Optional<Double> errorThreshold
    ) {
        try {
            return instrumenting(callable, name, errorThreshold).call();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public <T> T callChecked(
            Callable<T> callable,
            String name,
            Optional<Double> errorThreshold
    ) throws Exception {
        return instrumenting(callable, name, errorThreshold).call();
    }

    public void run(
            Runnable runnable,
            String name
    ) {
        run(runnable, name, Optional.empty());
    }

    public void runChecked(
            CheckedRunnable runnable,
            String name
    ) throws Exception {
        runChecked(runnable, name, Optional.empty());
    }

    public <T> T call(
            Callable<T> callable,
            String name
    ) {
        return call(callable, name, Optional.empty());
    }

    public <T> T callChecked(
            Callable<T> callable,
            String name
    ) throws Exception {
        return callChecked(callable, name, Optional.empty());
    }

}
