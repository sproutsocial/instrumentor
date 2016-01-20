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

    private Instrumentation() {}

}
