package com.sproutsocial.metrics;

import static org.junit.Assert.assertEquals;


import java.lang.reflect.Method;

import org.junit.Test;

/**
 * Created on 3/31/15
 *
 * @author horthy
 */
public class NamesTest {
    @Test
    public void testGetMethodName() throws Exception {
        final Method[] methods = NamesTest.class.getMethods();
        final Method method = methods[0];
        final String name = Names.name(method);
        assertEquals("com.sproutsocial.metrics.NamesTest.testGetMethodName", name);
    }
}
