# time-count

Most current programming libraries for time calculations are based on essentially the same model,
        JodaTime (now java.time in Java 8 and later) being one of the better examples.
Core concepts include instants, intervals, periods, and a few other concepts. Time values are wrappers
        around a Unix Time (a number of milliseconds since January 1970).
Chronologies are configurable in principle, but this doesn't usually happen

Time Count is intended as an exploration of alternative ways of thinking about
time for business software. It is NOT currently suited for use in production code.

See files /time-count/test/time_count/demo.clj
and /time-count/test/time_count/explainer.clj
for illustrations in code.


## Central Model Themes

### Everything an Interval

All time values are intervals. There are no "Instants".
So a date is an interval one :day long. A typical timestamp is an
interval one :millisecond long. There can be years, quarters or any
needed scale of interval.

### Allen's Interval Algebra for relations
Any interval can be compared with any other to produce one of Allen's relations.
Intervals are defined either as part of a sequence or as a "RelationBoundedInterval".

Each time in a sequence of countable times (see below) is an interval
that :meets the next interval in the sequence.
A relation-bounded interval, is specified by two smaller intervals,
one that :starts it and one that :finishes it. (A relation-bounded interval
may have only one of these relations, and be unbounded on the other side.
(In time-count, the lack of a bound is interpreted as an unknown bound, rather
than an infinite interval.)

### Time is countable
Calendars and time are (for most business software) weird ways of *counting*, not *measuring*.
All times are intervals, in sequences (so we can count forward and backward through them).
A sequence of intervals can be nested within an interval of a larger "scale".
E.g. Days are nested within months. Months are nested within years.

### Bidirectional String representation (mostly ISO 8601)
Having a string representation of a time is first-class concern, not just display formatting issue.
In time-count there is a string representation of every value, with bidirectional
operations to convert between the string and any other representation used in computations.
time-count uses ISO 8601, whenever there is a suitable representation,

### Other representations for computation (e.g. MetaJoda)
Most of the time, in this prototype, the computation-friendly representation
is 'meta-joda', which just adds some metadata to a JodaTime DateTime.
The metadata represents a nesting of scales intended to be significant.

MetaJoda is a place-holder implementation. It could be any representation that supports
a few primitives (specified in the protocol). For now, this lets us use a lot of work the JodaTime people have done!
E.g. How many days are in February in 2017? When does New York switch to Daylight Savings Time in 2017?


## UP NEXT
1. Change metajoda to use localtime until timezone handled right(?)
2. Finish basic readme.md
3. Rename repo to 'time-count' DONE
4. Make repo public
5. Basic timezone and daylight savings time cases.
6. Same day next month example.
