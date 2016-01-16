Even More Metrics
===================

*Instrumenting your code to protect your users*

Why Instrument
-----------------


You can't know what your code is doing unless you measure it. 
And if you don't know what your code is doing,
you can't know if you're letting your users down.

This point has been beaten to death, and a deep dive into the value of instrumenting
your code is outside the scope of this blog post. If you haven't already, I highly
reccomend Coda Hale's talk about the [Metrics](TODO) library he developed at yammer. Not only does it
deeply develop the "how" and "why" of instrumenting production code, but
it is a good introduction to the library upon which Sprout's instrumentation 
tooling relies heavily.

I don't want for this introduction to be a transcript of that talk, but if you haven't seen it,
I'll reproduce a small quote here that serves as a good summary:

|
| ...
| Somebody should get woken up about it
|


TODO Benefits:
    measure biz value
    diagnose failures before they happen

But instrumenting methods is hard. In order to acheive these benefits,
as developers we need to have foresight regarding what we want to measure. 
We need to be able to predict what kinds of deficiencies might affect our
applications, *before* they break our applications. A few examples that I've 
run into might include

- A network call that usually completes in about 200 milliseconds suddenly
begins taking up to 5 seconds to complete.
- A database insert that usually fails once every ten thousand calls begins to
fail once every hundred calls.
- A queue used by a large pool of consumers to asynchonously process messages
on the backend begins to slowly accumulate more and more queued messages, perhaps because
a downstream problem in consumers is causing messages to be processed more slowly.
- The call rate of an external-facing API endpoint that is generally 
called one hundred times per second drops down to one call per second, or even drops off
entirely. Its not clear whether a client is failing to make calls or 
if some underlying networking or proxy failure is making the endpoint unreachable.

TODO: THIS IS THE CORE VALUE PROP, INTRODUCTION, STATEMENT OF PURPOSE. IT SHOULD PROBABLY GO EARLIER IN THE WRITEUP.
At Sprout, we use a small set of java tools on top of [Metrics](TODO) to help us do this.
The core goal of [Instrumentor](TODO) is to minimize the amount of time developers have to spend
writing instrumentation code, while maximizing the amount of 
value developers and operations get from real-time metrics
and sophisticated application-level health checks.


Now, there's no cure-all solution. 
Only we understand the specifics of how our code creates value for our users. 
Only we can really know what parts of our app are critical if we want to avoid letting our users down.

That said, at Sprout we've found some things that we consistently want to measure
for almost every key method in our java codebase.














What we Report
---------------

For the sake of example, lets assume we're instrumenting the method below

```java
package com.mycompany;

class Example {
    public void sayHello() {
        System.out.println("Hello, World");
    }
}

```

If we were to  instrument the method `com.example.Example#sayHello`, an
`Instrumentor` would report a number of metrics related to the
timing, call rate, error rates, and a count of calls in flight. 

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

You'll get a Histogram of the timing of `sayHello`. That is, a statistical
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


An `Instrumentor` will also optionally create a [HealthCheck](TODO metrics healthchecks) that will monitor the percent of errors, and notify when the error rate exceeds the threshold.

Assuming threshold specified given is `0.1` i.e. ten percent, and there were 
five errors in the last 100 calls, the healtcheck would report as "Healthy".

If there were, however, twenty errors in the last 100 calls, the healthcheck
would report as "Unhealthy", with a message of `"value=0.2&threshold=0.1"`

The goal with healthchecks is to give us something that we can monitor to drive
pagerduty/nagios/zabbix/whatever we're using for monitoring and alerting.


**Note**

By default, the statistic used to determine the error percentage is `errors.m15_pct`.
This is not yet configurable, but its on the roadmap. The policy has been to lean toward
simplicity and sane defaults for now, and add configurability as it becomes clear what
needs to be configured.


Getting Started
-----------

You can include the latest version using maven:

```xml
<dependency>
    <groupId>com.sproutsocial</groupId>
    <artifactId>instrumentor</artifactId>
    <version>0.7.0</version
</dependency>
```

**Note**

At the time of writing, this isn't deployed to maven central. See [#1](TODO issue number).
In the mean time, you'll either have to install locally, or deploy to an
internal maven repository.





### Instrumenting with Instrumentor

### Instrumenting with Guice AOP

What this gets us
-------------------

TODO: IS THIS PARAGRAPH TOO PERSONAL? MEAN? VULNERABLE? REDUNDANT?
Its important to measure the right thing.
You can spend an extra afternoon instrumenting your new feature, 
only to find that when failures or deficiencies come around, 
the stats you're tracking aren't correlated to the deficiencieis.
So its hard to pitch the value of instrumentation. 
THE PITCH FOR THE REST OF THE WRITEUP IS "MEASURE MORE STUFF".
THE GOAL OF THE PRECEEDING PARAGRAPH IS TO PITCH THE VALUE OF "MEASURE MORE STUFF WITH LESS LINES OF CODE". I don't think anyone would disagree that less lines is good. gonna chuck to the end for now, maybe it will fit somewhere.

Contributing & Thanks
---------------

