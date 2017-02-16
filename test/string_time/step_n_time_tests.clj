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

(facts "Examples of composing time transformations and stringifiers"
       (fact (-> "2017-02-13" ((t-> identity))) => "2017-02-13")
       (fact (-> "2017-02-13" ((t-> (enclosing :month)))) => "2017-02")

       (fact "The composition can be a meaningful operation."
             (let [last-day-of-month (t-> (enclosing :month) (nested-last :day))]
               (last-day-of-month "2017-02-13")) => "2017-02-28"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;  Allen's Interval Algebra  ;;;
;;;  Basic relations           ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(facts "about Allen's Interval Algebra"
       (fact "Thirteen basic relations in Allen's Interval Algebra"
             (relation "2017" "2017") => :equal
             (relation "2015" "2017") => :before
             (relation "2017" "2015") => :after
             (relation "2016" "2017") => :meets
             (relation "2017" "2016") => :met-by
             (relation "2017-01" "2017") => :starts
             (relation "2017" "2017-01") => :started-by
             (relation "2017-12" "2017") => :finishes
             (relation "2017" "2017-12") => :finished-by
             (relation "2017-02" "2017") => :during
             (relation "2017" "2017-02") => :contains
             (relation "2017-W05" "2017-02") => :overlaps
             (relation "2017-02" "2017-W05") => :overlapped-by)

       (fact "If scales are different."
             (relation "2016-12" "2017") => :meets
             (relation "2016-11" "2017") => :before
             (relation "2017-W01" "2017") => :during))

(fact "example: is the invoice due"
      (let [net-30 (t-> interval-seq #(nth % 30))
            net-30-EOM (t-> (enclosing :month) next-interval (nested-last :day))
            overdue? (fn [terms completion-date today] (#{:after :met-by} (relation today (terms completion-date))))]

        (net-30 "2017-01-15") => "2017-02-14"
        (net-30-EOM "2017-01-15") => "2017-02-28"
        (overdue? net-30 "2017-01-15" "2017-02-10") => falsey
        (overdue? net-30 "2017-01-15" "2017-02-20" ) => truthy
        (overdue? net-30-EOM "2017-01-15" "2017-02-20" ) => falsey
        (overdue? net-30-EOM "2017-01-15" "2017-03-01") => truthy))