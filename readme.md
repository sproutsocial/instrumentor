Instrumentor
====================

Some handy instrumentation utilities for use with [Metrics](https://dropwizard.github.io/metrics/3.1.0/)


Instrumenting Methods 
---------------------

This module's primary aim is to increase the ease with which you
can monitor calls to the key methods in your java application. If you're not sure
what methods to instrument or why you should be instrumenting anything
in the first place, you should go check out [Coda Hale's Metrics Introduction](https://youtu.be/czes-oa0yik?t=6m29s)

The module has a number of tools for instrumenting methods. In general, an 
instrumented method will report timing, call rate, error rates, and a count of
call in flight.

Assume we have the class

```java
package com.mycompany;

class Example {
    public void sayHello() {
        System.out.println("Hello, World");
    }
    
    public static void main(String[] args) {
        new Example().sayHello();
    }
}

```

If we were to instrument the `sayHello` method, we'd get the following metrics registered and reported.


#### In Flight Count

You'll get a count of the current method calls in flight.

* `com.mycompany.Example.sayHello.inFlight.count` -- a count of method calls currently being executed

#### Call Rate

You'll get several stats for how often the `sayHello` method is being called.

* `com.mycompany.Example.sayHello.count` -- a lifetime count of calls to the method
* `com.mycompany.Example.sayHello.mean_rate` -- mean rate of calls
* `com.mycompany.Example.sayHello.m1_rate` -- rate of calls over the last 1 minute
* `com.mycompany.Example.sayHello.m5_rate` -- rate of calls over the last 5 minutes
* `com.mycompany.Example.sayHello.m15_rate` -- rate of calls over the last 15 minutes

#### Timing Statistics

You'll get a Histogram of the timing of your method. That is, a statistical
representation of the distribution of "How long did it take to call this method"

* `com.mycompany.Example.sayHello.max` -- maximum time it took to call the method
* `com.mycompany.Example.sayHello.mean` -- mean time to call the method
* `com.mycompany.Example.sayHello.min` -- minimum time it took to call the method
* `com.mycompany.Example.sayHello.p50` -- time it took for 50% of calls to complete
* `com.mycompany.Example.sayHello.p75` -- time it took for 75% of calls to complete
* `com.mycompany.Example.sayHello.p95` -- time it took for 95% of calls to complete
* `com.mycompany.Example.sayHello.p99` -- time it took for 99% of calls to complete
* `com.mycompany.Example.sayHello.p999` -- time it took for 99.9% of calls to complete
* `com.mycompany.Example.sayHello.stddev` -- standard deviation of method call times


#### Error Rate

You'll get stats to track the rate at which your method throws exceptions

* `com.mycompany.Example.sayHello.errors.count` -- a lifetime count of errors
* `com.mycompany.Example.sayHello.errors.mean_rate` -- mean rate of errors
* `com.mycompany.Example.sayHello.errors.m1_rate` -- rate of errors over the last 1 minute
* `com.mycompany.Example.sayHello.errors.m5_rate` -- rate of errors over the last 5 minutes
* `com.mycompany.Example.sayHello.errors.m15_rate` -- rate of errors over the last 15 minutes

#### Percent Error Rate
    
You'll get stats to track the percentage of method calls are throwing exceptions

* `com.mycompany.Example.sayHello.errors.total_pct` -- a lifetime percent of errors
* `com.mycompany.Example.sayHello.errors.mean_pct` -- mean percent of errors
* `com.mycompany.Example.sayHello.errors.m1_pct` -- percent of errors over the last 1 minute
* `com.mycompany.Example.sayHello.errors.m5_pct` -- percent of errors over the last 5 minutes
* `com.mycompany.Example.sayHello.errors.m15_pct` -- percent of errors over the last 15 minutes

#### HealthCheck

If you pass a `threshold` as the third argument, you'll get a HealthCheck that will monitor 
the percent of errors, and notify when the error rate exceeds the threshold.

Assuming the threshold given is `0.1` i.e. ten percent, and there were 
five errors in the last 100 calls, the healtcheck would report as "Healthy".

If there were, however, twenty errors in the last 100 calls, the healthcheck
would report as "Unhealthy", with a message of `"value=0.2&threshold=0.1"`

HealthChecks can by logged by interrogating the underlying `HealthCheckRegistry`.
Metrics has a good guide for HealthChecks [here](https://dropwizard.github.io/metrics/3.1.0/manual/healthchecks/).

The goal with healthchecks is to give us something that we can monitor to drive
pagerduty/nagios/zabbix/whatever we're using for monitoring and alerting.


**Note**

By default, the statistic used to determine the error percentage is `errors.m15_pct`.
This is not yet configurable, but its on the roadmap. The policy has been to lean toward
simplicity and sane defaults for now, and add configurability as it becomes clear what
needs to be configured.

How to Instrument
-----------------

#### Instrumenting with Instrumentor

The easiest way to instrument methods is using the `Instrumentor` class. 

To get it, you'll need to include `instrumentor-core`:

```
<dependency>
  <groupId>com.sproutsocial</groupId>
  <artifactId>instrumentor-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

Continuing the example from earlier, lets instrument the `sayHello` method. To instrument
methods that return `void`, you'll want to use `Instrumentor#run`.

To instrument the method, we'll need two things:

- an instance of `java.util.concurrent.Runnable`
- a name, `String` which will be the prefix for the generated metrics. 

##### Easier naming

In the example above, we used `"com.mycompany.Example.sayHello"` as the base name,
but there's nothing special about the name, it can be any string. 

If you're so inclined, you can use `MetricRegistry.name(Class<?> klass, String... names)`
to name your method:

```java
    String methodName = MetricRegistry.name(Example.class, "sayHello"); // "com.mycompany.Example.sayHello"
```

```java
package com.mycompany;

public class Example {
    public void sayHello() {
        System.out.println("Hello, World");
    }
    
    public static void main(String[] args) {
    
        Example example = new Example();

        String baseName = MetricRegistry.name(Example.class, "sayHello"); 
        Instrumentor instrumentor = new Instrumentor();
        
        Runnable runnable =  new Runnable() {
            public void run() {
                example.sayHello(); 
            }
        };
        
        // run the method
        instrumentor.run(runnable, baseName);
    }
}
```


##### With a lambda

With java 8 lambdas, the last few lines become a bit simpler.

Instead of 

```java
        
Runnable runnable =  new Runnable() {
    public void run() {
        example.sayHello(); 
    }
};

// run the method
instrumentor.run(runnable, baseName);
```

we can use 

```java
instrumentor.run(() -> example.sayHello(), baseName);
```

or even

```java
instrumentor.run(example::sayHello, baseName);
```

##### Adding a healthcheck

To add a healthcheck, supply an error threshold (`double`) as the third argument.

```java
instrumentor.run(example::sayHello, baseName, 0.1);
```

##### Instrumenting a callable

You can also instrument instances of `java.until.concurrent.Callable`:

```java
package com.mycompany;

public class Example {
    public String getGreeting() {
        return "Hello, World";
    }
    
    public static void main(String[] args) {
    
        Example example = new Example();

        String baseName = MetricRegistry.name(Example.class, "sayHello"); 
        Instrumentor instrumentor = new Instrumentor();
        
        
        // run the method
        String greeting = instrumentor.call(example::getGreeting, baseName); // "Hello, World"
    }
}
```

##### Inspecting Results

The no-arg constructor for `Instrumentor` will create its own
underlying `MetricRegistry` and `HealthCheckRegistry`. It exposes
them through accessors so you can inspect the results.



```java
package com.mycompany;

public class Example {
    public void sayHello() {
        System.out.println("Hello, World");
    }
    
    public static void main(String[] args) {
    
        Example example = new Example();

        String baseName = MetricRegistry.name(Example.class, "sayHello"); 
        Instrumentor instrumentor = new Instrumentor();
        
        // run the method
        instrumentor.run(example::sayHello, baseName);

        // get the MetricRegistry
        MetricRegistry metricRegistry = instrumentor.getMetricRegistry();

        Meter meter = metricRegistry.meter(baseName);

        assert meter.getCount() > 0;
    }
}
```


##### Supplying your own registries

If you want to supply your own registries, you can use `Instrumentor.Builder`


```java
package com.mycompany;

public class Example {
    public void sayHello() {
        System.out.println("Hello, World");
    }
    
    public static void main(String[] args) {
    
        Example example = new Example();

        String baseName = MetricRegistry.name(Example.class, "sayHello"); 

        // our own registries
        MetricRegistry metricRegistry = new MetricRegistry();
        HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();


        // pass into Instrumentor via Builder
        Instrumentor instrumentor = Instrumentor.builder()
                .metricRegistry(metricRegistry)
                .metricRegistry(healthCheckRegistry)
                .build();
        
        // run the method
        instrumentor.run(example::sayHello, baseName);

        Meter meter = metricRegistry.meter(baseName);

        assert meter.getCount() > 0;

        HealthCheck.Result result = healthCheckRegistry.runHealthCheck(baseName);

        assert result.isHealthy();
    }
}
```


Instrumenting with Guice AOP
----------------------------

There are also modules for Instrumenting using annotations and
[Guice AOP](https://github.com/google/guice/wiki/AOP).

You'll need to include the `instrumentor-aop` module:

```
<dependency>
  <groupId>com.sproutsocial</groupId>
  <artifactId>instrumentor-aop</artifactId>
  <version>1.0.0</version>
</dependency>
```

If you include an instance of the `InstrumentedAnnotations` module in your
call to `Guice.createInjector()` (see below), then you can use the `@Instrumented`
annotation to instrument methods. The module will also bind a `MetricRegistry`
and a `HealthCheckRegistry`.


```java
package com.mycompany;

class Example {
    @Instrumented
    public void sayHello() {
        System.out.println("Hello, World");
    }
    
    public static void main(String[] args) {
        Injector injector = Guice.createInjector(new InstrumentedAnnotations());
        
        Example example =  injector.getInstance(Example.class);
        
        example.sayHello();
        example.sayHello();
        
        
        MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);

        Timer timer = metricRegistry.getMeters().get("com.mycompany.Example.sayHello");

        assert timer.getOneMinuteRate() > 0;
        
        Meter meter = metricRegistry.getMeters().get("com.mycompany.Example.sayHello.errors");

        assert meter.getOneMinuteRate() == 0;
    }
}
```

#### Method names

By default, the methods annotated by `@Instrumented` method will be named

```
com.somepackage.ClassName.methodName
```

But that can be overriden using the `name` attribute of `Instrumented`:


```java
package com.mycompany;

class Example {
    @Instrumented(name="mySweetName")
    public void sayHello() {
        System.out.println("Hello, World");
    }
    
    public static void main(String[] args) {
        
        Injector injector = Guice.createInjector(new InstrumentedAnnotations());
        
        Example example =  injector.getInstance(Example.class);
        
        example.sayHello();
        example.sayHello();

        MetricRegistry metricRegistry = injector.getInstance(MetricRegistry.class);
        
        Timer timer = metricRegistry.getMeters().get("mySweetName");
        
        assert timer.getOneMinuteRate() > 0;
        
        Meter meter = metricRegistry.getMeters().get("mySweetName.errors");
        
        assert meter.getOneMinuteRate() == 0;
    }
}
```

#### Health Check

To add a healthcheck for the error percentage, just add an `errorThreshold`
to the `@Instrumented` annotation.


```java
package com.mycompany;

class Example {
    @Instrumented(errorThreshold=0.1)
    public void sayHello() {
        System.out.println("Hello, World");
    }
    
    public static void main(String[] args) {
        
        Injector injector = Guice.createInjector(new InstrumentedAnnotations());
        
        Example example =  injector.getInstance(Example.class);
        
        example.sayHello();
        example.sayHello();

        HealthCheckRegistry healthCheckRegistry = injector.getInstance(HealthCheckRegistry.class);
        
        HealthCheck.Result result = healthCheckRegistry.runHealthCheck(
            "com.mycompany.Example.sayHello"
        );
        
        assert result.isHealthy();
    }
}
```

##### Supplying your own registries

Like `Instrumentor`, `InstrumentedAnnotations` has a `Builder` that you can use 
to supply your own registries.

```java
package com.mycompany;

class Example {
    @Instrumented(errorThreshold=0.1)
    public void sayHello() {
        System.out.println("Hello, World");
    }
    
    public static void main(String[] args) {

        // our own registries
        MetricRegistry metricRegistry = new MetricRegistry();
        HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
        
        InstrumentedAnnotations annotationsModule = InstrumentedAnnotations.builder()
            .metricRegistry(metricRegistry)
            .healthCheckRegistry(healthCheckRegistry)
            .build();

        Injector injector = Guice.createInjector(annotationsModule);
        
        Example example =  injector.getInstance(Example.class);
        
        example.sayHello();
        example.sayHello();

        Timer timer = metricRegistry.getMeters().get("mySweetName");
        
        assert timer.getOneMinuteRate() > 0;
        
        HealthCheck.Result result = healthCheckRegistry.runHealthCheck(
            "com.mycompany.Example.sayHello"
        );
        
        assert result.isHealthy();
    }
}
```

##### AOP Gotchas

Not seeing metrics that you think you should be? There are a couple of gotchas with guice AOP.

1. AOP only works for methods being called by other classes. This is 
because Guice AOP works by inserting a proxy in front of the instrumented class
wherever it is injected. Calls of instrumented methods from within the same class
will not go through this proxy.

2. AOP will only be applied to objects created by guice. If you just use `new Example()`,
the annotation will have no effect.

3. You'll need to make sure that the registries used to build the `InstrumentedAnnotations`
module are the same as the ones you're reading from. 


Reporting Your Metrics
-----------------

Now that you're tracking metrics, you'll want to report them.

In dropwizard, metrics reporting can be configured in your yaml config, 
and you can inspect metrics and healthchecks using the admin servlet.

For non dropwizard applications, or to report to graphite see [The Metrics Documentation](https://dropwizard.github.io/metrics/3.1.0/manual/graphite/).

You can also embed your own [Admin Servlet](https://dropwizard.github.io/metrics/3.1.0/manual/servlets/).


