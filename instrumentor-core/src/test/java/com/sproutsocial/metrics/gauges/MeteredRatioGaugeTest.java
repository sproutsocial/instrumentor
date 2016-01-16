package com.sproutsocial.metrics.gauges;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metered;

/**
 * Created on 4/19/15
 *
 * @author horthy
 */
@RunWith(MockitoJUnitRunner.class)
public class MeteredRatioGaugeTest {
    
    private @Mock Metered numerator;
    private @Mock Metered denominator;
    
    @Test
    public void testGaugeDefaultRate() throws Exception {
        
        when(numerator.getOneMinuteRate()).thenReturn(10d);
        when(denominator.getOneMinuteRate()).thenReturn(100d);

        Gauge<Double> gauge = Gauges.ratioOf(
                numerator,
                denominator
        );
        
        assertEquals((Double) 0.1d, gauge.getValue()); // yay boxing
    }
    
    @Test
    public void testCustomAccessor() throws Exception {

        when(numerator.getFifteenMinuteRate()).thenReturn(10d);
        when(denominator.getFifteenMinuteRate()).thenReturn(100d);

        Gauge<Double> gauge = Gauges.ratioOf(
                numerator,
                denominator,
                Metered::getFifteenMinuteRate
        );

        assertEquals((Double) 0.1d, gauge.getValue()); // yay boxing
    }
    
    @Test
    public void testNaN() throws Exception {

        when(numerator.getFifteenMinuteRate()).thenReturn(10d);
        when(denominator.getFifteenMinuteRate()).thenReturn(0d);

        Gauge<Double> gauge = Gauges.ratioOf(
                numerator,
                denominator,
                Metered::getFifteenMinuteRate
        );

        assertEquals(Double.NaN, (Object) gauge.getValue()); // yay boxing
    }
}
