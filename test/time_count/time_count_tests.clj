(ns time-count.time-count-tests
  (:require [time-count.time-count :refer :all]
            [time-count.meta-joda :refer [same-time?]]
            [midje.sweet :refer :all])
  (:import [org.joda.time DateTime]))

(facts "about interval sequences"
       (fact "A named time interval is part of a sequence of intervals of that scale,
       with can be traversed with next-interval."
             (next-interval [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])
             => [(DateTime. 2017 1 11 0 0 0 0) :day :month :year]
             (next-interval [(DateTime. 2016 12 31 0 0 0 0) :day :month :year])
             => [(DateTime. 2017 1 1 0 0 0 0) :day :month :year])

       (fact "We can make a bounded sequence."
             (take 3 (interval-seq [(DateTime. 2016 12 31 0 0 0 0) :day :month :year]))
             => [[(DateTime. 2016 12 31 0 0 0 0) :day :month :year]
                 [(DateTime. 2017 1 1 0 0 0 0) :day :month :year]
                 [(DateTime. 2017 1 2 0 0 0 0) :day :month :year]]

             (interval-seq [(DateTime. 2016 12 31 0 0 0 0) :day :month :year]
                           [(DateTime. 2017 1 2 0 0 0 0) :day :month :year])
             => [[(DateTime. 2016 12 31 0 0 0 0) :day :month :year]
                 [(DateTime. 2017 1 1 0 0 0 0) :day :month :year]
                 [(DateTime. 2017 1 2 0 0 0 0) :day :month :year]])

       ;; TODO Decide on style of representation of intervals
       (fact "A time interval is equivalent to a bounded sequence."
             (interval-seq2 {:starts [(DateTime. 2016 12 31 0 0 0 0) :day :month :year] :finishes [(DateTime. 2017 1 2 0 0 0 0) :day :month :year]})
             => [[(DateTime. 2016 12 31 0 0 0 0) :day :month :year]
                 [(DateTime. 2017 1 1 0 0 0 0) :day :month :year]
                 [(DateTime. 2017 1 2 0 0 0 0) :day :month :year]])



       (fact "In meta-joda, we ignore any value of scales below those specified (impl detail)."
             (same-time? [(DateTime. 2017 4 30 0 0 0 0) :day :month :year]
                         [(DateTime. 2017 4 30 1 2 3 4) :day :month :year])
             => truthy
             (same-time? [(DateTime. 2017 4 30 0 0 0 0) :day :month :year]
                         [(DateTime. 2017 4 29 1 2 3 4) :day :month :year])
             => falsey)



       (future-fact "Members of an interval sequence are at the same scale."
                    ((interval-seq [(DateTime. 2016 12 31 0 0 0 0) :day :month :year]
                                   [(DateTime. 2017 3 1 0 0 0 0) :month :year])
                      => :no-match)))

(facts "about a convenience function for operations of ISO 8601 -> ISO 8601"
       (fact (t-> "2017-04-30" identity) => "2017-04-30")
       (fact (t-> "2017-04-30" next-interval) => "2017-05-01"))

(facts "about moving up the hierarchy of scales"
       (fact "A time is nested within its enclosing intervals of higher scales."
             (t-> "2017-04-30" (enclosing :month)) => "2017-04"
             (t-> "2017-04-30" (enclosing :year)) => "2017")
       (fact "The default is the immediately higher scale."
             (t-> "2017-04-30" (enclosing)) => "2017-04"
             (t-> "2017-04" (enclosing)) => "2017")
       (fact "Implementation detail: In meta-joda, we can just change scale, since we ignore any value of scales below those specified."
             ((enclosing) [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])
             => [(DateTime. 2017 1 10 0 0 0 0) :month :year]
             ((enclosing :year) [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])
             => [(DateTime. 2017 1 10 0 0 0 0) :year])
       (future-fact "We might ask for an enclosing scale that isn't there."
                    ((enclosing) [(DateTime. 2017 1 1 0 0 0 0) :year])
                    => :no-match
                    ((enclosing :week) [(DateTime. 2017 1 1 0 0 0 0) :month :year])
                    => :no-match))


(facts "about nesting sequences of smaller-scale intervals."
       (fact "When nesting a smaller scale within an interval,
                the bounds of that interval are expressed
                in terms of the smaller scale."
             (t-> "2017-02" (nested-first :day)) => "2017-02-01"
             ((nested-first :day) [(DateTime. 2017 2 1 0 0 0 0) :month :year])
             => [(DateTime. 2017 2 1 0 0 0 0) :day :month :year]

             (t-> "2017-02" (nested-last :day)) => "2017-02-28"
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

(facts "Examples of composing time transformations and stringifiers"
       (fact (t-> "2017-02-13" identity) => "2017-02-13")
       (fact (t-> "2017-02-13" (enclosing :month)) => "2017-02")

       (fact "The composition can be a meaningful, higher-level operation."
             (let [last-day-of-month (comp (nested-last :day) (enclosing :month))]
               (t-> "2017-02-13"  last-day-of-month)) => "2017-02-28"))

