package com.sproutsocial.metrics.healthchecks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.codahale.metrics.Gauge;

/**
 * Created on 4/19/15
 *
 * @author horthy
 */
@RunWith(MockitoJUnitRunner.class)
public class GaugeHealthCheckTest {
    private @Mock Gauge<Double> doubleGauge;
    private @Mock Gauge<String> stringGauge;

    private GaugeHealthCheck<Double> healthCheck;
    
    private GaugeHealthCheck<String> stringHealthCheck;

    @Test
    public void testHealthy() throws Exception {
        when(doubleGauge.getValue()).thenReturn(3d);

        healthCheck = HealthChecks.forDoubleGauge(doubleGauge, 5d);

        assertTrue(healthCheck.check().isHealthy());
    }
    
    @Test
    public void testUnhealthyAtThreshold() throws Exception {
        when(doubleGauge.getValue()).thenReturn(5d);

        healthCheck = HealthChecks.forDoubleGauge(doubleGauge, 5d);

        assertFalse(healthCheck.check().isHealthy());
    }
    
    @Test
    public void testUnhealthy() throws Exception {
        when(doubleGauge.getValue()).thenReturn(10d);

        healthCheck = HealthChecks.forDoubleGauge(doubleGauge, 5d);

        assertFalse(healthCheck.check().isHealthy());
    }
    
    @Test
    public void testNaN() throws Exception {
        when(doubleGauge.getValue()).thenReturn(Double.NaN);

        healthCheck = HealthChecks.forDoubleGauge(doubleGauge, 5d);

        assertTrue(healthCheck.check().isHealthy());
    }
    
    @Test
    public void testRegularGauge() throws Exception {
        when(stringGauge.getValue()).thenReturn("foobarbaz");

        stringHealthCheck = HealthChecks.forGauge(stringGauge, "foobarbazzz");

        assertTrue(stringHealthCheck.check().isHealthy());
    }
    
    @Test
    public void testRegularGaugeUnhealthy() throws Exception {
        when(stringGauge.getValue()).thenReturn("foobarbaz");

        stringHealthCheck = HealthChecks.forGauge(stringGauge, "abc");

        assertFalse(stringHealthCheck.check().isHealthy());
    }
}
