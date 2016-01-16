package com.sproutsocial.metrics;

import java.util.concurrent.Callable;
import java.util.function.Predicate;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.base.Throwables;
import com.sproutsocial.metrics.gauges.Gauges;
import com.sproutsocial.metrics.healthchecks.HealthChecks;

/**
 * Created on 4/17/15
 *
 * @author horthy
 */
public class Instrumentor {

    private final MetricRegistry metricRegistry;
    private final HealthCheckRegistry healthCheckRegistry;
    private final Predicate<Throwable> filter;

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
        this.healthCheckRegistry = healthCheckRegistry;
        this.filter = filter;
    }

    /**
     * essentially no-op, handy for testing
     */
    public Instrumentor() {
        this(new MetricRegistry(), new HealthCheckRegistry(), any -> true);
    }

    public <T> Callable<T> instrumenting(
            Callable<T> callable,
            String name,
            double threshold
    ) {

        final Meter errorMeter = metricRegistry.meter(name + ".errors");
        final Timer timer = metricRegistry.timer(name);
        final Counter inFlight = metricRegistry.counter(name + ".inFlight");

        if (!errorGaugesExist(metricRegistry, name)) {
            registerErrorGauges(metricRegistry, name, errorMeter, timer);
        }

        if (threshold > 0 && !healthCheckExists(healthCheckRegistry, name)) {
            registerHealthCheck(healthCheckRegistry, name, threshold, errorMeter, timer);
        }

        return () -> {
            inFlight.inc();
            try (Timer.Context ctx = timer.time()){
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
            double threshold
    ) {

        final Meter errorMeter = metricRegistry.meter(name + ".errors");
        final Timer timer = metricRegistry.timer(name);
        final Counter inFlight = metricRegistry.counter(name + ".inFlight");

        if (!errorGaugesExist(metricRegistry, name)) {
            registerErrorGauges(metricRegistry, name, errorMeter, timer);
        }

        if (threshold > 0 && !healthCheckExists(healthCheckRegistry, name)) {
            registerHealthCheck(healthCheckRegistry, name, threshold, errorMeter, timer);
        }

        return () -> {
            inFlight.inc();
            try (Timer.Context ctx = timer.time()){
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
            double threshold
    ) {

        final Meter errorMeter = metricRegistry.meter(name + ".errors");
        final Timer timer = metricRegistry.timer(name);
        final Counter inFlight = metricRegistry.counter(name + ".inFlight");

        if (!errorGaugesExist(metricRegistry, name)) {
            registerErrorGauges(metricRegistry, name, errorMeter, timer);
        }

        if (threshold > 0 && !healthCheckExists(healthCheckRegistry, name)) {
            registerHealthCheck(healthCheckRegistry, name, threshold, errorMeter, timer);
        }

        return () -> {
            inFlight.inc();
            try (Timer.Context ctx = timer.time()){
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

    public void run(
        Runnable runnable,
        String name
    ) {
        run(runnable, name, -1d);
    }

    public void run(
            Runnable runnable,
            String name,
            double threshold
    ) {
        instrumenting(runnable, name, threshold).run();
    }

    public void runChecked(
        CheckedRunnable runnable,
        String name
    ) throws Exception {
        runChecked(runnable, name, -1d);
    }

    public void runChecked(
            CheckedRunnable runnable,
            String name,
            double threshold
    ) throws Exception {
        instrumenting(runnable, name, threshold).run();
    }

    public <T> T call(
        Callable<T> callable,
        String name
    ) {
        return call(callable, name, -1d);
    }

    public <T> T call(
            Callable<T> callable,
            String name,
            double threshold
    ) {
        try {
            return instrumenting(callable, name, threshold).call();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public <T> T callChecked(
        Callable<T> callable,
        String name
    ) throws Exception {
        return callChecked(callable, name, -1d);
    }

    public <T> T callChecked(
            Callable<T> callable,
            String name,
            double threshold
    ) throws Exception {
        return instrumenting(callable, name, threshold).call();
    }



    /* package scoped static methods, re-used in MethodInterceptor */

    static boolean healthCheckExists(HealthCheckRegistry healthCheckRegistry, String name) {
        return healthCheckRegistry.getNames().contains(name);
    }

    /* package */ static boolean errorGaugesExist(MetricRegistry metricRegistry, String name) {
        return metricRegistry
                .getGauges()
                .containsKey(MetricRegistry.name(name, "errors", "mean_pct"));
    }

    /* package */ static void registerHealthCheck(HealthCheckRegistry healthCheckRegistry, String name, double threshold, Metered errorMeter, Metered timer) {
        final Gauge<Double> errorRate = Gauges.ratioOf(errorMeter, timer, Metered::getFifteenMinuteRate);

        final HealthCheck healthCheck = HealthChecks.forDoubleGaugeNaNisHealthy(
                errorRate,
                threshold
        );
        healthCheckRegistry.register(name, healthCheck);
    }

    /* package */ static void registerErrorGauges(MetricRegistry metricRegistry, String name, Meter errorMeter, Timer timer) {
        final Gauge<Double> totalErrorRate = Gauges.ratioOf(errorMeter, timer, m -> new Long(m.getCount()).doubleValue());
        final Gauge<Double> meanErrorRate = Gauges.ratioOf(errorMeter, timer, Metered::getMeanRate);
        final Gauge<Double> m1ErrorRate = Gauges.ratioOf(errorMeter, timer, Metered::getOneMinuteRate);
        final Gauge<Double> m5ErrorRate = Gauges.ratioOf(errorMeter, timer, Metered::getFiveMinuteRate);
        final Gauge<Double> m15ErrorRate = Gauges.ratioOf(errorMeter, timer, Metered::getFifteenMinuteRate);

        tryRegister(totalErrorRate, MetricRegistry.name(name, "errors", "total_pct"), metricRegistry);
        tryRegister(meanErrorRate, MetricRegistry.name(name, "errors", "mean_pct"), metricRegistry);
        tryRegister(m1ErrorRate, MetricRegistry.name(name, "errors", "m1_pct"), metricRegistry);
        tryRegister(m5ErrorRate, MetricRegistry.name(name, "errors", "m5_pct"), metricRegistry);
        tryRegister(m15ErrorRate, MetricRegistry.name(name, "errors", "m15_pct"), metricRegistry);
    }

    /**
     * There's a Potential race condition with
     * the check to {@link Instrumentor#errorGaugesExist(MetricRegistry, String)} and when
     * we actually go to register the gauge, so lets be extra careful here
     * and put it in a try-catch.
     *
     * This is a little hacky.
     */
    /* package */ static void tryRegister(Gauge<Double> meanErrorRate, String name, MetricRegistry metricRegistry) {
        try {
            metricRegistry.register(name, meanErrorRate);
        } catch (IllegalArgumentException ignoreAlreadyRegistered)  {}
    }

}
