package com.sproutsocial.metrics;

import java.util.Optional;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.sproutsocial.metrics.gauges.Gauges;
import com.sproutsocial.metrics.healthchecks.HealthChecks;

/**
 * Created on 1/16/16
 *
 * @author horthy
 */
/* package */ final class Instrumentation {

    public static final double NO_THRESHOLD_DEFINED = -1d;

    private Instrumentation() {}

    static <T> boolean shouldRegisterHealthCheck(Optional<HealthCheckRegistry> healthCheckRegistry, String name, Optional<T> ceiling) {
        return healthCheckRegistry.isPresent() &&
                ceiling.isPresent() &&
                !healthCheckExists(healthCheckRegistry.get(), name);
    }

    static boolean healthCheckExists(HealthCheckRegistry healthCheckRegistry, String name) {
        return healthCheckRegistry.getNames().contains(name);
    }

    static boolean errorGaugesExist(MetricRegistry metricRegistry, String name) {
        return metricRegistry
                .getGauges()
                .containsKey(MetricRegistry.name(name, "errors", "mean_pct"));
    }

    static void registerHealthCheck(HealthCheckRegistry healthCheckRegistry, String name, Optional<Double> ceiling, Metered errorMeter, Metered timer) {
        final Gauge<Double> errorRate = Gauges.ratioOf(errorMeter, timer, Metered::getFifteenMinuteRate);

        final HealthCheck healthCheck = HealthChecks.forDoubleGauge(
                errorRate,
                ceiling
        );
        healthCheckRegistry.register(name, healthCheck);
    }

    static void registerErrorGauges(MetricRegistry metricRegistry, String name, Meter errorMeter, Timer timer) {
        final Gauge<Double> totalErrorPct = Gauges.ratioOf(errorMeter, timer, m -> Long.valueOf(m.getCount()).doubleValue());
        final Gauge<Double> meanErrorPct = Gauges.ratioOf(errorMeter, timer, Metered::getMeanRate);
        final Gauge<Double> m1ErrorPct = Gauges.ratioOf(errorMeter, timer, Metered::getOneMinuteRate);
        final Gauge<Double> m5ErrorPct = Gauges.ratioOf(errorMeter, timer, Metered::getFiveMinuteRate);
        final Gauge<Double> m15ErrorPct = Gauges.ratioOf(errorMeter, timer, Metered::getFifteenMinuteRate);

        tryRegister(totalErrorPct, MetricRegistry.name(name, "errors", "total_pct"), metricRegistry);
        tryRegister(meanErrorPct, MetricRegistry.name(name, "errors", "mean_pct"), metricRegistry);
        tryRegister(m1ErrorPct, MetricRegistry.name(name, "errors", "m1_pct"), metricRegistry);
        tryRegister(m5ErrorPct, MetricRegistry.name(name, "errors", "m5_pct"), metricRegistry);
        tryRegister(m15ErrorPct, MetricRegistry.name(name, "errors", "m15_pct"), metricRegistry);
    }

    /**
     * There's a Potential race condition with
     * the check to {@link Instrumentation#errorGaugesExist(MetricRegistry, String)} and when
     * we actually go to register the gauge, so lets be extra careful here
     * and put it in a try-catch.
     *
     * This is a little hacky.
     */
    static void tryRegister(Gauge<Double> meanErrorRate, String name, MetricRegistry metricRegistry) {
        try {
            metricRegistry.register(name, meanErrorRate);
        } catch (IllegalArgumentException ignoreAlreadyRegistered)  {}
    }
}
