(ns time-count.contrast-with-joda
    (:require [time-count.core :refer :all]
              [time-count.iso8601 :refer [t->]]
              [time-count.metajoda] ;Must have SOME impl in namespace
              [midje.sweet :refer :all])
  (:import [org.joda.time DateTime Days Months]))

;; Time libraries usually view time as a measure
(fact "add a period to an instant"
      (.plus (DateTime. 2017 3 5 0 0 0 0) (Days/days 5))
      => (DateTime. 2017 3 10 0 0 0 0)
      (.plus (DateTime. 2017 3 5 0 0 0 0) (Months/months 1))
      => (DateTime. 2017 4 5 0 0 0 0)
      (.plus (DateTime. 2017 3 31 0 0 0 0) (Months/months 1))
      ; What do you expect? (Scroll down to see)



      => (DateTime. 2017 4 30 0 0 0 0)
      (.plus (DateTime. 2017 4 30 0 0 0 0) (Months/months 1))
      => (DateTime. 2017 5 30 0 0 0 0)
      (.plus (DateTime. 2017 3 31 0 0 0 0) (Months/months 2))
      => (DateTime. 2017 5 31 0 0 0 0))

;; What is a "month"? What is an "instant"?
;; A range of vaguely defined cases... (not least before/after)

;; In time-count:
;; All times are intervals (no "Instants")
;; Intervals have a scale, which corresponds to "Period"
;; but is not used in the same way.


(fact "the particular ambiguity (of add months) goes away with counting"
      (let [later (fn [n] #(-> {:starts %} t-sequence (nth n)))]
        (t-> "2017-04-19" ((later 5))) => "2017-04-24"
        (t-> "2017-03" ((later 1))) => "2017-04")
        ; To add months to a :day :month :year, you have
        ; to explicitly say what that actually means
        ;   (let [same-day-next-month ...]
        ;       march 5 plus month
        ;       march 31 plus month => nil)
        ; If we want the jodatime behavior, we can say so
        ; (let [next-month-same-day-or-eom (or ...)
        ;  march 5 +
        ;  march 31 +
        )
