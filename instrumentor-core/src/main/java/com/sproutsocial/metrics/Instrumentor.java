package com.sproutsocial.metrics;

import java.util.Optional;
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

public class Instrumentor {

    /* package */ static final double NO_THRESHOLD_DEFINED = -1d;

    private final MetricRegistry metricRegistry;
    private final HealthCheckRegistry healthCheckRegistry;
    private final Predicate<Throwable> exceptionFilter;


    private class Context {
        private Meter totalMeter;
        private Meter successMeter;
        private Meter errorMeter;
        private Timer timer;
        private Counter inFlight;

        Context(String name) {
            totalMeter = metricRegistry.meter(name + ".totalStarted");
            successMeter = metricRegistry.meter(name + ".success");
            errorMeter = metricRegistry.meter(name + ".errors");
            timer = metricRegistry.timer(name);
            inFlight = metricRegistry.counter(name + ".inFlight");
        }
    }

    public static class Builder {
        private MetricRegistry metricRegistry = new MetricRegistry();
        private HealthCheckRegistry healthCheckRegistry = null;
        private Predicate<Throwable> filter = ExceptionFilters.markAllExceptions();

        private Builder() {}

        public Builder metricRegistry(MetricRegistry metricRegistry) {
            this.metricRegistry = metricRegistry;
            return this;
        }

        public Builder healthCheckRegistry(HealthCheckRegistry healthCheckRegistry) {
            this.healthCheckRegistry = healthCheckRegistry;
            return this;
        }

        public Builder exceptionFilter(Predicate<Throwable> filter) {
            this.filter = filter;
            return this;
        }

        public Instrumentor build() {
            return new Instrumentor(metricRegistry, healthCheckRegistry, filter);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public Instrumentor() {
        this(new MetricRegistry(), new HealthCheckRegistry(), any -> true);
    }

    public Instrumentor(
            MetricRegistry metricRegistry,
            HealthCheckRegistry healthCheckRegistry,
            Predicate<Throwable> exceptionFilter) {
        this.metricRegistry = metricRegistry;
        this.healthCheckRegistry = healthCheckRegistry;
        this.exceptionFilter = exceptionFilter;
    }

    /**
     * @return the underlying {@link HealthCheckRegistry}
     */
    public HealthCheckRegistry getHealthCheckRegistry() {
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
            context.totalMeter.mark();
            context.inFlight.inc();
            T result;
            try (@SuppressWarnings("unused") Timer.Context ctx = context.timer.time()){
                result = callable.call();
            } catch (Exception e) {
                if (exceptionFilter.test(e)) {
                    context.errorMeter.mark();
                }
                throw e;
            } finally {
                context.inFlight.dec();
            }
            context.successMeter.mark();
            return result;
        };
    }

    private <T> ThrowableCallable<T> instrumenting(
            ThrowableCallable<T> callable,
            String name,
            Optional<Double> errorThreshold
    ) {

        final Context context = createInstrumentationContext(name, errorThreshold);

        return () -> {
            context.totalMeter.mark();
            context.inFlight.inc();
            T result;
            try (@SuppressWarnings("unused") Timer.Context ctx = context.timer.time()){
                result = callable.call();
            } catch (Throwable e) {
                if (exceptionFilter.test(e)) {
                    context.errorMeter.mark();
                }
                throw e;
            } finally {
                context.inFlight.dec();
            }
            context.successMeter.mark();
            return result;
        };
    }

    private CheckedRunnable instrumenting(
            CheckedRunnable runnable,
            String name,
            Optional<Double> errorThreshold
    ) {

        final Context context = createInstrumentationContext(name, errorThreshold);

        return () -> {
            context.totalMeter.mark();
            context.inFlight.inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = context.timer.time()){
                runnable.run();
            } catch (Exception e) {
                if (exceptionFilter.test(e)) {
                    context.errorMeter.mark();
                }
                throw e;
            } finally {
                context.inFlight.dec();
            }
            context.successMeter.mark();
        };
    }

    private Runnable instrumenting(
            Runnable runnable,
            String name,
            Optional<Double> errorThreshold
    ) {

        final Context context = createInstrumentationContext(name, errorThreshold);

        return () -> {
            context.totalMeter.mark();
            context.inFlight.inc();
            try (@SuppressWarnings("unused") Timer.Context ctx = context.timer.time()){
                runnable.run();
            } catch (Exception e) {
                if (exceptionFilter.test(e)) {
                    context.errorMeter.mark();
                }
                throw e;
            } finally {
                context.inFlight.dec();
            }
            context.successMeter.mark();
        };
    }

    private Context createInstrumentationContext(String name, Optional<Double> errorThreshold) {
        final Context context = new Context(name);
        if (!errorGaugesExist(name)) {
            registerErrorGauges(name, context.errorMeter, context.timer);
        }

        if (shouldRegisterHealthCheck(name, errorThreshold)) {
            registerHealthCheck(name, errorThreshold, context.errorMeter, context.timer);
        }
        return context;
    }

    private <T> boolean shouldRegisterHealthCheck(String name, Optional<T> ceiling) {
        return healthCheckRegistry != null &&
                ceiling.isPresent() &&
                !healthCheckExists(name);
    }

    private boolean healthCheckExists(String name) {
        return healthCheckRegistry.getNames().contains(name);
    }

    private boolean errorGaugesExist(String name) {
        return metricRegistry
                .getGauges()
                .containsKey(MetricRegistry.name(name, "errors", "mean_pct"));
    }

    private void registerHealthCheck(String name, Optional<Double> ceiling, Metered errorMeter, Metered timer) {
        final Gauge<Double> errorRate = Gauges.ratioOf(errorMeter, timer, Metered::getFifteenMinuteRate);

        final HealthCheck healthCheck = HealthChecks.forDoubleGauge(
                errorRate,
                ceiling
        );

        healthCheckRegistry.register(name, healthCheck);
    }

    private void registerErrorGauges(String name, Meter errorMeter, Timer timer) {
        final Gauge<Double> totalErrorPct = Gauges.ratioOf(errorMeter, timer, m -> Long.valueOf(m.getCount()).doubleValue());
        final Gauge<Double> meanErrorPct = Gauges.ratioOf(errorMeter, timer, Metered::getMeanRate);
        final Gauge<Double> m1ErrorPct = Gauges.ratioOf(errorMeter, timer, Metered::getOneMinuteRate);
        final Gauge<Double> m5ErrorPct = Gauges.ratioOf(errorMeter, timer, Metered::getFiveMinuteRate);
        final Gauge<Double> m15ErrorPct = Gauges.ratioOf(errorMeter, timer, Metered::getFifteenMinuteRate);

        tryRegister(totalErrorPct, MetricRegistry.name(name, "errors", "total_pct"));
        tryRegister(meanErrorPct, MetricRegistry.name(name, "errors", "mean_pct"));
        tryRegister(m1ErrorPct, MetricRegistry.name(name, "errors", "m1_pct"));
        tryRegister(m5ErrorPct, MetricRegistry.name(name, "errors", "m5_pct"));
        tryRegister(m15ErrorPct, MetricRegistry.name(name, "errors", "m15_pct"));
    }

    /**
     * There's a Potential race condition with
     * the check to {@link this#errorGaugesExist(String)} and when
     * we actually go to register the gauge, so lets be extra careful here
     * and put it in a try-catch.
     *
     * This is a little hacky.
     */
    private void tryRegister(Gauge<Double> meanErrorRate, String name) {
        try {
            metricRegistry.register(name, meanErrorRate);
        } catch (IllegalArgumentException ignoreAlreadyRegistered)  {}
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


    /* package */ <T> T callThrowably(
            ThrowableCallable<T> callable,
            String name,
            Optional<Double> errorThreshold
    ) throws Throwable {
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

    /* package */ <T> T callThrowably(
            ThrowableCallable<T> callable,
            String name
    ) throws Throwable {
        return callThrowably(callable, name, Optional.empty());
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

    /* package */ <T> T callThrowably(
            ThrowableCallable<T> callable,
            String name,
            double errorThreshold
    ) throws Throwable {
        return callThrowably(callable, name, Optional.of(errorThreshold));
    }




}
