package com.sproutsocial.metrics;

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

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public abstract class BaseInstrumentor {
    /* package */ static final double NO_THRESHOLD_DEFINED = -1d;
    protected final MetricRegistry metricRegistry;
    protected final HealthCheckRegistry healthCheckRegistry;
    protected final Predicate<Throwable> exceptionFilter;

    protected BaseInstrumentor(MetricRegistry metricRegistry, HealthCheckRegistry healthCheckRegistry, Predicate<Throwable> exceptionFilter) {
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

    protected abstract <T> Callable<T> instrumenting(
            Callable<T> callable,
            String name,
            Optional<Double> errorThreshold
    );

    protected abstract <T> ThrowableCallable<T> instrumenting(
            ThrowableCallable<T> callable,
            String name,
            Optional<Double> errorThreshold
    );

    protected abstract CheckedRunnable instrumenting(
            CheckedRunnable runnable,
            String name,
            Optional<Double> errorThreshold
    );

    abstract Runnable instrumenting(
            Runnable runnable,
            String name,
            Optional<Double> errorThreshold
    );

    protected abstract InstrumentorContext getInstrumentatorContext(String name);

    protected InstrumentorContext createInstrumentationContext(String name, Optional<Double> errorThreshold) {
        final InstrumentorContext context = getInstrumentatorContext(name);
        if (!errorGaugesExist(name)) {
            registerErrorGauges(name, context.getErrorMeter(), context.getTimer());
        }

        if (shouldRegisterHealthCheck(name, errorThreshold)) {
            registerHealthCheck(name, errorThreshold, context.getErrorMeter(), context.getTimer());
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
     * <p>
     * This is a little hacky.
     */
    private void tryRegister(Gauge<Double> meanErrorRate, String name) {
        try {
            metricRegistry.register(name, meanErrorRate);
        } catch (IllegalArgumentException ignoreAlreadyRegistered) {
        }
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
