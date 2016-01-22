package com.sproutsocial.metrics.healthchecks;

import java.util.Optional;
import java.util.Set;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.health.HealthCheck;
import com.google.common.collect.ImmutableSet;

/**
 * Created on 4/19/15
 *
 * A {@link HealthCheck} that reports healthy as long as the underlying
 * {@link Gauge}
 * has a value less than a ceiling value, or included in a whitelist.
 *
 * @author horthy
 */
public class GaugeHealthCheck<T extends Comparable<T>> extends HealthCheck {

    private final Gauge<T> gauge;
    private final Optional<T> ceiling;
    private final Set<T> alwaysHealthy;

    public GaugeHealthCheck(Gauge<T> gauge, Optional<T> ceiling, Set<T> alwaysHealthy) {
        this.gauge = gauge;
        this.ceiling = ceiling;
        this.alwaysHealthy = alwaysHealthy;
    }
    
    public GaugeHealthCheck(Gauge<T> gauge, Optional<T> ceiling) {
        this(gauge, ceiling, ImmutableSet.of());
    }

    @Override
    protected Result check() throws Exception {
        final T value = gauge.getValue();
        
        // allow short-circuit
        if (alwaysHealthy.contains(value)) {
            return Result.healthy();
        }

        return ceiling.map(value::compareTo).orElse(-1) < 0 ?
                Result.healthy() :
                Result.unhealthy(getUnhealthyMessage(value));

    }

    private String getUnhealthyMessage(T value) {
        return "value=" + value + "&ceiling=" + ceiling;
    }
}
