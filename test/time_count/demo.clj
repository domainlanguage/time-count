(ns time-count.demo
  (:require [time-count.time-count :refer :all]
            [time-count.allens-interval-algebra :refer [relation]]
            [time-count.meta-joda :refer [stringify destringify]]
            [midje.sweet :refer :all])
  (:import [java.util.Date]
           [org.joda.time DateTime Days Months]))


;; Time libraries usually view time as a measure
(fact "add a period to an instant"
      (.plus (DateTime. 2017 3 5 0 0 0 0) (Days/days 5))
      => (DateTime. 2017 3 10 0 0 0 0)
      (.plus (DateTime. 2017 3 5 0 0 0 0) (Months/months 1))
      => (DateTime. 2017 4 5 0 0 0 0)
      (.plus (DateTime. 2017 3 31 0 0 0 0) (Months/months 1))
      ; What do you expect?



      => (DateTime. 2017 4 30 0 0 0 0)
      (.plus (DateTime. 2017 4 30 0 0 0 0) (Months/months 1))
      => (DateTime. 2017 5 30 0 0 0 0)
      (.plus (DateTime. 2017 3 31 0 0 0 0) (Months/months 2))
      => (DateTime. 2017 5 31 0 0 0 0))

;; What is a "month"? What is an "instant"?
;; A range of vaguely defined cases... (not least before/after)

;; Here's a different model:
;;  Calendars and time are (for most business software) weird ways of *counting*, not *measuring*.
;;  All times are intervals, in sequences (so we can count them).
;;  A sequence of intervals can be nested within an interval of a larger "scale".
;;  Relations between intervals can be well defined.
;; Also,
;;  Having a string representation of a time may be important.
;;  I'm using ISO, and have operations to go between the string and the form I compute on.


;; Here is a representation of a time, which just adds some metadata to a JodaTime DateTime.
;; The metadata represents a nesting of scales intended to be significant.
;;
;; This is a place-holder implementation. It could be any representation that supports
;; a few primitives (described below). For now, this lets me use a lot of work the JodaTime people have done!
;;
;; Also, having string representations o

(fact "Time representation needs metadata representing nested scale"
      (-> "2017-04-09" destringify) => [(DateTime. 2017 4 9 0 0 0 0) :day :month :year]
      (-> "2017-04-09T11:17" destringify rest) => [:minute :hour :day :month :year]
      (-> "2017-04" destringify rest) => [:month :year])


;;Now, treating all times as intervals has some implications.
(fact "An interval is part of a sequence, so next is meaningful"
      (let [next-str (t-> next-interval)]
        (next-str "2017-04-09") => "2017-04-10"
        (next-str "2017-04") => "2017-05"
        (next-str "2017") => "2018"
        (next-str "2017-04-09T11:17") => "2017-04-09T11:18"
        (next-str "2017-02-28") => "2017-03-01"
        (next-str "2016-02-28") => "2016-02-29"
        (next-str "2017-070") => "2017-071"
        (next-str "2017-365") => "2018-001"
        (next-str "2017-W52") => "2018-W01"))

(fact "Intervals can be nested within an interval of a larger scale"
      (-> "2017-04" destringify ((nested-seq :day)) count) => 30
      (-> "2017" destringify ((nested-seq :day)) count) => 365
      (-> "2016" destringify ((nested-seq :day)) count) => 366
     ; :week-year is still a puzzle (-> "2017" destringify ((nested-seq :week)) count) => 52?
      (-> "2017" destringify ((nested-seq :month)) count) => 12
      (-> "2017" destringify ((nested-seq :month)) first stringify) => "2017-01"
      ((t-> (nested-seq :month) first) "2017") => "2017-01"
      ((t-> (nested-seq :month) last) "2017") => "2017-12"
      )


(fact "an interval sequence can be nested within an interval of a higher scale."
            (-> "2017-04-09" destringify ((enclosing)) stringify)
            => "2017-04"
            (-> "2017-04-09" destringify ((enclosing)) next-interval stringify)
            => "2017-05"
            ((t-> (enclosing) next-interval) "2017-04-09")
            => "2017-05")

(fact "Common transformations can be composed from these operations."
      (let [later (fn [n t] ((t-> interval-seq #(nth % n)) t))
            last-day (t-> (nested-seq :day) last)
            eom (t-> (enclosing :month) (nested-seq :day) last)]

        (later 5 "2017-04-19") => "2017-04-24"
        (later 5 "2017") => "2022"
        (last-day "2017-04") => "2017-04-30"
        (last-day "2017") => "2017-365"
      ;  (last-day "2017-04-19") => TODO How to represent stuff like this?
        (eom "2017-04-19") => "2017-04-30"
        (eom "2017-04-19T15:12") => "2017-04-30"))
        ; A more complex eom could preserve nesting
        ; and find last interval of same scale, etc.

; Business rules can be composed from these basic operations.
(fact " Example: invoice due"
      (let [net-30 (t-> interval-seq #(nth % 30))
            net-30-EOM (t-> (enclosing :month) next-interval (nested-last :day))
            overdue? (fn [terms completion-date today] (#{:after :met-by} (relation today (terms completion-date))))]

        (net-30 "2017-01-15") => "2017-02-14"
        (net-30-EOM "2017-01-15") => "2017-02-28"
        (overdue? net-30 "2017-01-15" "2017-02-10T14:30") => falsey
        (overdue? net-30 "2017-01-15" "2017-02-20" ) => truthy
        (overdue? net-30-EOM "2017-01-15" "2017-02-20" ) => falsey
        (overdue? net-30-EOM "2017-01-15" "2017-03-01") => truthy))

;; All times are intervals (no "Instants")
;; Intervals have a scale, which corresponds to "Period"
;; Don't think of measuring time, think of a complicated counting system.
;; A calendar is a sequence of intervals of scale "day"
;; A sequence of days can be nested inside a sequence of months, within a sequence of years.
;; Or a sequence of days can be nested inside a sequence of quarters within a sequence of years.
;; Or days within years or days within weeks.
;; In a sequence of months, the scale is month. It doesn't matter that not all are the same length. They are all a month!
;; If you nest days (named with numbers) within a month, then some februaries have 28 and some have 29.
;; String representation should be seamless (using ISO 8601 where possible)



