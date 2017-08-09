(ns time-count.explainer
  (:require
    [time-count.time-count :refer :all]
    [time-count.iso-8601 :refer :all]
    [midje.sweet :refer :all]))

(fact "A convenience function allows application of time transforming functions with ISO 8601 strings."
      ;; This may be mostly for tests and demos. Perhaps it will be used in some apps.
      (t-> "2017-04-30" identity) => "2017-04-30")

(fact
  (->> "2017-04" iso-to-mj interval-seq (take 3) (map mj-to-iso)))

(future-fact "In time-count, there is no 'instant'. Only sequences of intervals."
      (t-> "2017-04-09" next-interval) => "2017-04-10"
      (t-> "2017-04" next-interval) => "2017-05"
      (t-> "2017-04" interval-seq (take 3)) => "x")

