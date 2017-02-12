(ns string-time.step-n-time-tests
  (:require [string-time.step-n-time :refer :all]
            [string-time.meta-joda :refer [same-time?]]
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



       (future-fact "Members of an interval sequence are at the same scale."
                    ((interval-seq [(DateTime. 2016 12 31 0 0 0 0) :day :month :year]
                                   [(DateTime. 2017 3 1 0 0 0 0) :month :year])
                      => :no-match)))


(facts "about nested scales"

       (fact "We can move up the hierarchy of scales."
             ((enclosing) [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])
             => [(DateTime. 2017 1 10 0 0 0 0) :month :year]
             ((enclosing) [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])
             => #(same-time? % [(DateTime. 2017 1 1 0 0 0 0) :month :year])
             ((enclosing :year) [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])
             => #(same-time? % [(DateTime. 2017 1 1 0 0 0 0) :year]))

       (future-fact "We might ask for an enclosing scale that isn't there."
                    ((enclosing) [(DateTime. 2017 1 1 0 0 0 0) :year])
                    => :no-match
                    ((enclosing :week) [(DateTime. 2017 1 1 0 0 0 0) :month :year])
                    => :no-match))

(facts "about nesting sequences of smaller-scale intervals."
       (fact "When nesting a smaller scale within an interval,
                the bounds of that interval must be expressed
                in terms of the smaller scale."
             ((nested-first :day) [(DateTime. 2017 2 1 0 0 0 0) :month :year])
             => [(DateTime. 2017 2 1 0 0 0 0) :day :month :year]
             ((nested-last :day) [(DateTime. 2017 2 1 0 0 0 0) :month :year])
             => [(DateTime. 2017 2 28 0 0 0 0) :day :month :year])

       (fact "We can nest another scale within an interval."
             ((nested-seq :day) [(DateTime. 2017 2 1 0 0 0 0) :month :year])
             => (has-prefix [[(DateTime. 2017 2 1 0 0 0 0) :day :month :year]
                             [(DateTime. 2017 2 2 0 0 0 0) :day :month :year]])
             (-> [(DateTime. 2017 2 1 0 0 0 0) :month :year]
                 ((nested-seq :day))
                 count)
             => 28))


