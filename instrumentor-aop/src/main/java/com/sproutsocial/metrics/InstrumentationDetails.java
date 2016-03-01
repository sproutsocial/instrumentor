package com.sproutsocial.metrics;

import com.google.common.base.Strings;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;

/**
 * Created on 2/29/16.
 */
public interface InstrumentationDetails {

    Instrumented getAnnotation(MethodInvocation methodInvocation);
    String name(Method method, Instrumented annotation);

    class ClassInstrumentation implements InstrumentationDetails {

        @Override
        public Instrumented getAnnotation(MethodInvocation methodInvocation) {
            return methodInvocation
                    .getMethod()
                    .getDeclaringClass()
                    .getAnnotation(Instrumented.class);
        }

        @Override
        public String name(Method method, Instrumented annotation) {

            String annotationName = annotation.name();

            if (Strings.isNullOrEmpty(annotationName)) {
                return Names.name(method);
            } else {
                return annotationName + method.getName();
            }
        }
    }

    class MethodInstrumentation implements InstrumentationDetails {

        @Override
        public Instrumented getAnnotation(MethodInvocation methodInvocation) {
            return methodInvocation
                    .getMethod()
                    .getDeclaredAnnotation(Instrumented.class);
        }

        @Override
        public String name(Method method, Instrumented annotation) {
            String annotationName = annotation.name();

            if (Strings.isNullOrEmpty(annotationName)) {
                return Names.name(method);
            } else {
                return annotationName;
            }
        }
    }

}
