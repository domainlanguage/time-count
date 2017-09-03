(ns time-count.time-count-tests
  (:require [time-count.time-count :refer :all]
            [time-count.meta-joda :refer :all]
            [time-count.iso-8601-old :refer :all]
            [midje.sweet :refer :all])
  (:import [org.joda.time DateTime]))



(facts "about a convenience function for operations of ISO 8601 -> ISO 8601"
       (fact (t-> "2017-04-30" identity) => "2017-04-30")
       (fact (t-> "2017-04-30" next-interval) => "2017-05-01"))

(facts "about moving up the hierarchy of scales"
       (fact "A time is nested within its enclosing-immediate intervals of higher scales."
             (t-> "2017-04-30" enclosing-immediate) => "2017-04"
             (t-> "2017-04" enclosing-immediate) => "2017"
             (t->> "2017-04-30" (enclosing :month)) => "2017-04"
             (t->> "2017-04-30" (enclosing :year)) => "2017")

       (fact "Passing the scale only returns a specialized enclosing-immediate function."
             (let [enclosing-year (enclosing :year)]
               (t-> "2017-04" enclosing-year) => "2017"
               (t-> "2017-04-30" enclosing-year) => "2017"))

       (fact "Implementation detail: In meta-joda, we can just change scale, since we ignore any value of scales below those specified."
             (enclosing-immediate [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])
             => [(DateTime. 2017 1 10 0 0 0 0) :month :year]
             ((enclosing :year) [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])
             => [(DateTime. 2017 1 10 0 0 0 0) :year])
       (future-fact "We might ask for an enclosing-immediate scale that isn't there."
                    (enclosing-immediate [(DateTime. 2017 1 1 0 0 0 0) :year])
                    => :no-match
                    (enclosing :week [(DateTime. 2017 1 1 0 0 0 0) :month :year])
                    => :no-match
                    (enclosing :year [(DateTime. 2017 1 1 0 0 0 0) :month :year])
                    => :no-match))                          ;Not sure on this last one!


(facts "about nesting sequences of smaller-scale intervals."
       (fact "When nesting a smaller scale within an interval,
                the bounds of that interval are expressed
                in terms of the smaller scale."
             (t->> "2017-02" (nested-first :day)) => "2017-02-01"
             ((nested-first :day) [(DateTime. 2017 2 1 0 0 0 0) :month :year])
             => [(DateTime. 2017 2 1 0 0 0 0) :day :month :year]

             (t->> "2017-02" (nested-last :day)) => "2017-02-28"
             ((nested-last :day) [(DateTime. 2017 2 1 0 0 0 0) :month :year])
             => [(DateTime. 2017 2 28 0 0 0 0) :day :month :year])

       (fact "The result can be represented as a sequence of the smaller scale."
             ((nested-seq :day) [(DateTime. 2017 2 1 0 0 0 0) :month :year])
             => (has-prefix [[(DateTime. 2017 2 1 0 0 0 0) :day :month :year]
                             [(DateTime. 2017 2 2 0 0 0 0) :day :month :year]])
             (-> [(DateTime. 2017 2 1 0 0 0 0) :month :year]
                 ((nested-seq :day))
                 count)
             => 28))

(fact "Nested scales of a named time are explicit.
       The value of any other scale is ignored"
      (same-time? [(DateTime. 2017 1 10 0 0 0 0) :day :month :year]
                  [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])
      => true
      (same-time? [(DateTime. 2017 1 10 0 0 0 0) :day :month :year]
                  [(DateTime. 2017 1 11 0 0 0 0) :day :month :year])
      => false
      (same-time? [(DateTime. 2017 1 10 0 0 0 0) :month :year]
                  [(DateTime. 2017 1 11 0 0 0 0) :month :year])
      => true
      (same-time? [(DateTime. 2017 1 10 0 0 0 0) :day :month :year]
                  [(DateTime. 2017 1 10 0 0 0 0) :month :year])
      => false)

(facts "about place values"
       (fact "Each scale has a place value"
             (place-value :month [(DateTime. 2017 2 28 0 0 0 0) :day :month :year])
             => 2)
       (fact "The time value could be represented as a map of place-values"
             (place-values [(DateTime. 2017 2 28 0 0 0 0) :day :month :year])
             => [:day 28 :month 2 :year 2017]))


(facts "Examples of composing time transformations and stringifiers"
       (fact (t-> "2017-02-13" identity) => "2017-02-13")
       (fact (t->> "2017-02-13" (enclosing :month)) => "2017-02")

       (fact "The composition can be a meaningful, higher-level operation."
             (let [last-day-of-month (comp (nested-last :day) (enclosing :month))]
               (t-> "2017-02-13" last-day-of-month)) => "2017-02-28"))

(facts "about mapping between nestings"
       (fact "mapping is bidirectional"
             (t->> "2017-04-25" (to-nesting [:day :week :week-year]))
             => "2017-W17-2"
             (t->> "2017-W17-2" (to-nesting [:day :month :year]))
             => "2017-04-25"))


(future-fact "Daylight savings time"
             (t-> "2016-11-06T01:59" next-interval)
             => "?")
