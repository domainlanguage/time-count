(ns time-count.explainer
  (:require
    [time-count.core :refer :all]
    [time-count.iso8601 :refer :all]
    [time-count.metajoda]
    [midje.sweet :refer :all]))

(fact "A convenience function allows application of time transforming functions with ISO 8601 strings."
      ;; This may be mostly for tests and demos. Perhaps it will be used in some apps.
      (t-> "2017-04-30" identity) => "2017-04-30")

(fact "In time-count, there is no 'instant'. Only sequences of intervals."
      (t-> "2017-04-09" next-interval) => "2017-04-10"
      (t-> "2017-04" next-interval) => "2017-05"
      (t->> "2017-04/-" t-sequence (take 3)) => ["2017-04" "2017-05" "2017-06"])

(fact "Business rules involving time are expressed using Allen's Interval Algebra."
      (letfn [(invoice-overdue? [due-date as-of] (#{:after :met-by} (relation as-of due-date)))]
        (t->> ["2017-02-15" "2017-02-10T14:30"] (apply invoice-overdue?)) => falsey
        (t->> ["2017-02-15" "2017-02-15T20:30"] (apply invoice-overdue?)) => falsey
        (t->> ["2017-02-15" "2017-02-15T23:59"] (apply invoice-overdue?)) => falsey
        (t->> ["2017-02-15" "2017-02-16T00:00"] (apply invoice-overdue?)) => truthy
        (t->> ["2017-02-15" "2017-02-17T10:04"] (apply invoice-overdue?)) => truthy))

(fact "Some nestings can be mapped to each other, bidirectionally"
      (t-> "2017-04-25" (to-nesting [:day :week :week-year]))
      => "2017-W17-2"
      (t-> "2017-W17-2" (to-nesting [:day :month :year]))
      => "2017-04-25")

(future-fact "Daylight savings time"
             (t-> "2016-11-06T01:59" next-interval)
             => "?")
