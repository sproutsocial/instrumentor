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

    private class Context {
        private Meter errorMeter;
        private Timer timer;
        private Counter inFlight;

        Context(String name) {
            errorMeter = metricRegistry.meter(name + ".errors");
            timer = metricRegistry.timer(name);
            inFlight = metricRegistry.counter(name + ".inFlight");
        }

    }

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

    private <T> Callable<T> instrumenting(
            Callable<T> callable,
            String name,
            Optional<Double> errorThreshold
    ) {

        final Context context = createInstrumentationContext(name, errorThreshold);

        return () -> {
            context.inFlight.inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = context.timer.time()){
                return callable.call();
            } catch (Exception e) {
                if (filter.test(e)) {
                    context.errorMeter.mark();
                }
                throw e;
            } finally {
                context.inFlight.dec();
            }
        };
    }

    private CheckedRunnable instrumenting(
            CheckedRunnable runnable,
            String name,
            Optional<Double> errorThreshold
    ) {

        final Context context = createInstrumentationContext(name, errorThreshold);

        return () -> {
            context.inFlight.inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = context.timer.time()){
                runnable.run();
            } catch (Exception e) {
                if (filter.test(e)) {
                    context.errorMeter.mark();
                }
                throw e;
            } finally {
                context.inFlight.dec();
            }
        };
    }

    private Runnable instrumenting(
            Runnable runnable,
            String name,
            Optional<Double> errorThreshold
    ) {

        final Context context = createInstrumentationContext(name, errorThreshold);

        return () -> {
            context.inFlight.inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = context.timer.time()){
                runnable.run();
            } catch (Exception e) {
                if (filter.test(e)) {
                    context.errorMeter.mark();
                }
                throw e;
            } finally {
                context.inFlight.dec();
            }
        };
    }

    private Context createInstrumentationContext(String name, Optional<Double> errorThreshold) {
        final Context context = new Context(name);
        if (!errorGaugesExist(metricRegistry, name)) {
            registerErrorGauges(metricRegistry, name, context.errorMeter, context.timer);
        }

        if (shouldRegisterHealthCheck(healthCheckRegistry, name, errorThreshold)) {
            registerHealthCheck(healthCheckRegistry.get(), name, errorThreshold, context.errorMeter, context.timer);
        }
        return context;
    }

    public <T> Callable<T> instrumenting(
            Callable<T> callable,
            String name,
            double errorThreshold
    ) {
        return instrumenting(callable, name, Optional.of(errorThreshold));
    }

    public CheckedRunnable instrumenting(
            CheckedRunnable runnable,
            String name,
            double errorThreshold
    ) {
        return instrumenting(runnable, name, Optional.of(errorThreshold));
    }

    public Runnable instrumenting(
            Runnable runnable,
            String name,
            double errorThreshold
    ) {
        return instrumenting(runnable, name, Optional.of(errorThreshold));
    }

    private void run(
            Runnable runnable,
            String name,
            Optional<Double> errorThreshold
    ) {
        instrumenting(runnable, name, errorThreshold).run();
    }

    private void runChecked(
            CheckedRunnable runnable,
            String name,
            Optional<Double> errorThreshold
    ) throws Exception {
        instrumenting(runnable, name, errorThreshold).run();
    }

    private <T> T call(
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

    private <T> T callChecked(
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


    public void run(
            Runnable runnable,
            String name,
            double errorThreshold
    ) {
        run(runnable, name, Optional.of(errorThreshold));
    }

    public void runChecked(
            CheckedRunnable runnable,
            String name,
            double errorThreshold
    ) throws Exception {
        runChecked(runnable, name, Optional.of(errorThreshold));
    }

    public <T> T call(
            Callable<T> callable,
            String name,
            double errorThreshold
    ) {
        return call(callable, name, Optional.of(errorThreshold));
    }

    public <T> T callChecked(
            Callable<T> callable,
            String name,
            double errorThreshold
    ) throws Exception {
        return callChecked(callable, name, Optional.of(errorThreshold));
    }

}
