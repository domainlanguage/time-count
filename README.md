# time-count

Most current programming libraries for time calculations are based on essentially the same model,
        JodaTime (now java.time in Java 8 and later) being one of the better examples.
Core concepts include instants, intervals, periods, and a few other concepts. Time values are wrappers
        around a Unix Time (a number of milliseconds since January 1970).
Chronologies are configurable in principle, but this doesn't usually happen

Time Count is an exploration of alternative ways of thinking about
time for business software. It is NOT currently recommended for use
in production code.

For examples in code, and more detailed explanations,
see files /time-count/explainers/time_count/explainer/*
(probably best read in order).

For a discussion of the broad exploration of time models
this was part of, check out the video here:
https://youtu.be/Zm95cYAtAa8

## Central Model Themes
1. Time is Countable
2. Everything is an Interval
3. Allen's Interval Algebra
4. Canonical String Representation (~ ISO 8601)
5. Composible Transformations

### Time is countable
Calendars and time are (for most business software) weird ways of *counting*.
E.g. at the most basic level, dates are a count of days:

    (t-> "2017-04-09" next-t)
    => "2017-04-10"

Viewing time as sequences nested within other sequences allows us to use
basic sequence logic to express many common time operations.
E.g. Days can be nested within months, and months within years.

    (t-> "2017-04-09"
       (enclosing :month))
    => "2017-04"

End of month could be expressed as:

    (t-> "2017-04-09
       (enclosing :month)
       (nest :day) t-sequence
       last))  ;Note that 'last' is the normal Clojure sequence operation.

     => "2017-04-30"

In time-count we avoid thinking of time units as a *measure*, as when we
might talk about "addition" of a period to a time.
There are domains where time as a measure is important
(e.g. keeping records for a track-meet), but that might be
better handled with a whole different time library focused on that.

See more examples in this file:
time_count/explainer/b_counting_and_nesting.clj

### Everything is an Interval
All time values are intervals. There are no "Instants".
So, for example, a date is an interval one day long. A typical meeting
invitation specifies the start time to the minute. A time value
could be a specific second, hour, month, year or quarter.

The sequences of countable times are sequences of consecutive intervals,
where each interval meets the interval following it.

An important case is the timestamp, which is usually regarded as an
"instant". But a timestamp is actually a reading from a physical clock.
If that clock ticks each millisecond, then there is an approximately
one millisecond-long interval during which you would get a particular
reading from the clock. Then there is another millisecond interval in
which you'd get the next value, and so on.
In time-count, a typical timestamp is represented as an interval,
one millisecond long. If the precision of the clock finer or coarser
than this, the appropriate scale could be chosen. (Of course, most time
stamps come from outside, so this is the time-count interpretation of
what they mean.

### Allen's Interval Algebra for relations
Allen's Interval Algebra is an elegant formalism for describing and
inferring the relations between different time intervals, and it is used
in time-count for comparing and defining time values.

Allen defined 13 basic relations that distinctly and exhaustively
relate any two intervals. These include the familiar "before" and "after",
but defined unambiguously. It also has relations like "meets" and
overlaps.

Example pseudocode (where 2016 is substituted for a value for the year 2016, etc.):

     (relation 2016 2018) => :before
     (relation 2016 2017) => :meets

Any interval can be compared with any other to produce one of Allen's relations.
Intervals are defined either as part of a sequence or as a "RelationBoundedInterval".

In a sequence of countable times, each time is an interval
that *meets* the next interval in the sequence.

A relation-bounded interval, is specified by two smaller intervals,
one that *starts* it and one that *finishes* it.

    (relation 2016 2016/2018) => :starts

(A relation-bounded interval may have only one of these relations,
and be unbounded on the other side. In time-count, the lack of a bound
is interpreted as an unknown bound -- not as an infinite interval.)

See more examples in file:
time_count/explainer/c_allens_interval_algebra.clj
And a good general explanation at
https://www.ics.uci.edu/~alspaugh/cls/shr/allen.html

### Canonical String representation (mostly ISO 8601)
Having a string representation of a time is first-class concern in time-count,
not just display formatting issue. There is a string representation of
every time value, with bidirectional operations to convert between the string
and any other representation used in computations (isomorphic).
time-count uses ISO 8601, whenever there is a suitable representation,

    (-> "2017-12-29T17:46"
        from-iso
        to-iso)
    => "2017-12-29T17:46"

For convenience, two threading macros are provided that accept the string
representations as input to time functions and produce strings from the results.

    (t-> "2017-04-09" next-t)
    => "2017-04-10"

See more examples in file:
time_count/stringifying_tests.clj

### Other representations for computation (e.g. MetaJodaTime)
time-count is not dependent on a specific representation of time-values,
but it includes three:
1. canonical strings (based on ISO 8601)
2. metajoda
3. place-values

Most of the time, in this prototype, the computation-friendly representation
is MetaJodaTime, which just adds some metadata to a JodaTime DateTime.
This metadata represents a nesting of scales intended to be significant.

MetaJodaTime (in metajoda namespace) is a place-holder implementation.
It could be any representation that supports a few low-level functions
specified in the CountableTime protocol and the Interval protocol.

Using  lets us use a lot of work the JodaTime people have done! E.g.
- How many days are in February in 2017?
- When does New York switch to Daylight Savings Time in 2017?
This embedded knowledge is accessed through the basic sequence and nesting functions.

Unintuitively, this means that to use operations like next-t you must
require the core namespace (where CountableTime is defined) and also
the metajoda namespace, because there must be some implementation of the
protocol. (You could provide another implementation instead.)

Another representation is "place values".
For example,

    (place-values (from-iso "2017-12-28"))
    => [[:day 28] [:month 12] [:year 2017]]

(This representation might actually be an alternative for implementing
nest and enclosing.)

See more examples in this file:
time_count/explainer/a_representations.clj

### Composible Transformations
This is not a very unusual goal. It is important, though,
and not always adhered to in creating time libraries.
Instead of creating all the operations people will want
when using the library, time-count tries to create a small
set of low-level operations that compose well. Each project
could then create their own higher-level operations.

For example, "end-of-month" might be a function provided in
a library, but it is not provided in time-count. It can be
composed (as described in the Countable section above) from
the basic sequence and nesting operations:

    (defn end-of-month [t]
      (-> t (enclosing :month)
            (nest :day)
            t-sequence
            last))

    (t-> "2017-04-09" end-of-month)
    => "2017-04-30"

    (t-> "2017-04-09T17:46" end-of-month)
    => "2017-04-30"


This is easier partly *because time-count connects time with
the standard sequence abstraction of the programming language*,
which supports all sorts of rich operations and compositions.
(This is true of many modern languages.)

Also, concepts like "end-of-month" may be defined differently
on different projects. If a business defines end-of-month
to mean the last *business day* of the month, they could
define a predicate, business-day?, and end-of-month as

    (defn end-of-month [t]
      (-> t (enclosing :month)
            (nest :day) t-sequence
            (#(filter business-day? %)
            last))
or in some other way suited to their business rules.

See more examples in this file:
time_count/explainer/d_composing_operations.clj

## Up Next
No firm plan, but probably,
1. Millisecond as a single-level nesting representing a
Unix time (milliseconds since the Epoch), and mapable to
a millisecond-scale time with a UTC-offset.
2. Representing timezones and offsets with the place-value
representation (which currently only represents the
local time component).


