package com.sproutsocial.metrics.healthchecks;

import java.util.Optional;

import com.codahale.metrics.Gauge;
import com.google.common.collect.ImmutableSet;

/**
 * Created on 4/19/15
 *
 * @author horthy
 */
public final class HealthChecks {
   
    private HealthChecks(){}
    
    public static <T extends Comparable<T>> GaugeHealthCheck<T> forGauge(
            Gauge<T> gauge, Optional<T> ceiling
    ) {
        return new GaugeHealthCheck<>(gauge, ceiling);
    }

    /*
     * creates a healthcheck for a double gauge, where
     * {@link Double#NaN} is considered to be healthy
     * 
     * This is useful for gauges that measure an error rate,
     * we don't want to blow up if the call rate is 0
     *
     */
    public static GaugeHealthCheck<Double> forDoubleGauge(
            Gauge<Double> gauge, Optional<Double> ceiling
    ) {
        return new GaugeHealthCheck<>(
                gauge, ceiling, ImmutableSet.of(Double.NaN));
    }
}
