(ns time-count.demo
  (:require [time-count.core :refer :all]
            [time-count.iso8601 :refer [to-iso from-iso t-> t->>]]
            [time-count.metajoda :refer [->MetaJodaTime]]
            [midje.sweet :refer :all])
  (:import [org.joda.time DateTime]))


;; Don't think of measuring time, think of a complicated counting system.
;; A calendar is a sequence of intervals of scale "day"
;; A sequence of days can be nested inside a sequence of months, within a sequence of years.
;; Or a sequence of days can be nested inside a sequence of quarters within a sequence of years.
;; Or days within years or days within weeks.
;; In a sequence of months, the scale is month. It doesn't matter that not all are the same length. They are all a month!
;; If you nest days (named with numbers) within a month, then some februaries have 28 and some have 29.
;; String representation should be seamless (using ISO 8601 where possible)



;; Here's a different model:
;;  Calendars and time are (for most business software) weird ways of *counting*, not *measuring*.
;;  All times are intervals, in sequences (so we can count forward and backward through them).
;;  A sequence of intervals can be nested within an interval of a larger "scale".
;;  E.g. Days are nested within months. Months are nested within years.
;;  Relations between intervals can be well defined.

;; String representation: ISO 8601
;;  Having a string representation of a time is important.
;;  time-count uses ISO 8601, whenever there is a suitable representation,
;;  and has operations to go between the string and the representation used in computations.

;; MetaJoda (for computation)
;;  Most of the time, that computation-friendly representation is 'meta-joda',
;;  which just adds some metadata to a JodaTime DateTime.
;;  The metadata represents a nesting of scales intended to be significant.
;;
;;  This is a place-holder implementation. It could be any representation that supports
;;  a few primitives (described below). For now, this lets us use a lot of work the JodaTime people have done!
;;  E.g. How many days are in February in 2017? When does New York switch to Daylight Savings Time in 2017?
;;


(fact "Time representation needs metadata representing nested scale"
      (-> "2017-04-09" from-iso) => (->MetaJodaTime (DateTime. 2017 4 9 0 0 0 0) [:day :month :year])
      (to-iso (->MetaJodaTime (DateTime. 2017 4 9 0 0 0 0) [:day :month :year])) => "2017-04-09"
      (-> "2017-04-09T11:17" from-iso :nesting) => [:minute :hour :day :month :year]
      (-> "2017-04" from-iso :nesting) => [:month :year])


(fact "A convenience macro allows application of time transforming functions with ISO 8601 strings."
      ;; This may be mostly for tests and demos. Perhaps it will be used in some apps for data interchange.
      (t-> "2017-04-30" identity) => "2017-04-30"
      ;     (t->> "2017-04-30" identity) => "2017-04-30")
      )

;;Treating all times as intervals has some implications.
(fact "An interval is part of a sequence, so next is meaningful"
      (t-> "2017-04-09" next-interval) => "2017-04-10"
      (t-> "2017-04" next-interval) => "2017-05"
      (t-> "2017" next-interval) => "2018"
      (t-> "2017-04-09T11:17" next-interval) => "2017-04-09T11:18"
      (t-> "2017-02-28" next-interval) => "2017-03-01"
      (t-> "2016-02-28" next-interval) => "2016-02-29"
      (t-> "2017-070" next-interval) => "2017-071"
      (t-> "2017-365" next-interval) => "2018-001"
      (t-> "2017-W52" next-interval) => "2018-W01")

(fact "A sequence can be nested within an interval of a larger scale"
      (t-> "2017-04" (nest :day) t-sequence count) => 30
      (t-> "2017" (nest :day) t-sequence count) => 365
      (t-> "2016" (nest :day) t-sequence count) => 366
      ; :week-year is still a puzzle (t-> "2017" (nest :week)) t-sequence count) => 52?
      (t-> "2017" (nest :month) t-sequence count) => 12
      (t-> "2017" (nest :month) t-sequence first) => "2017-01"
      (t-> "2017" (nest :month) t-sequence second) => "2017-02"
      (t-> "2017" (nest :month) t-sequence last) => "2017-12")

(fact "a member of an interval sequence nested within a larger interval"
      (t-> "2017-04-09" enclosing-immediate) => "2017-04"
      (t-> "2017-04-09" (enclosing :year)) => "2017"
      (t-> "2017-04-09" (enclosing :day)) => "2017-04-09")


(facts "about composing higher-level time operations from the basic protocol."
       (let [later (fn [n] #(-> {:starts %} t-sequence (nth n)))]
         (t-> "2017-04-19" ((later 5))) => "2017-04-24"
         (t-> "2017-04" ((later 5))) => "2017-09"
         (t-> "2017" ((later 5))) => "2022")

       (let [last-day #(-> % (nest :day) t-sequence last)
             last-day2 #(-> % (nest :day) :finishes)]
         (t-> "2017-04" last-day) => "2017-04-30"
         (t-> "2017" last-day) => "2017-365"
         (t-> "2017-04" last-day2) => "2017-04-30")

       (let [last-day #(-> % (nest :day) t-sequence last)
             eom #(-> % (enclosing :month) last-day)]
         (t-> "2017-04-19" eom) => "2017-04-30"
         (t-> "2017-04" eom) => "2017-04-30"
         (t-> "2017-04-19T15:12" eom) => "2017-04-30"))
; A more complex eom could preserve nesting
; and find last interval of same scale, etc.


; Business rules can be composed from these basic operations.
(fact " Example: invoice due"
      (let [net-30 (fn [t] (-> t (enclosing :day) (#(t-sequence {:starts %})) (nth 30)))
            eom #(-> % (enclosing :month) (nest :day) :finishes)
            net-30-eom (comp eom net-30)
            overdue? (fn [due-date t] (#{:after :met-by} (relation t due-date)))]

        (t-> "2017-01-15" net-30) => "2017-02-14"
        (t-> "2017-01-20" eom) => "2017-01-31"
        (t-> "2017-01-15T17:00" net-30-eom) => "2017-02-28"

        (let [work-completion (from-iso "2017-01-15T17:00")
              t1 (from-iso "2017-02-20T14:30")
              t2 (from-iso "2017-03-01T09:15")]
          (overdue? (net-30-eom work-completion) t1) => falsey
          (overdue? (net-30-eom work-completion) t2) => truthy)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Building up functions and then deriving holidays ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn day-of-week [ymd]
(-> ymd
    (to-nesting [:day :week :week-year])
    place-values
    (#(into {} %))
    :day))

(defn thursday? [ymd]
  (-> ymd
      day-of-week
      (= 4)))

(defn november [year]
  (-> year
      (nest :month)
      t-sequence
      (nth 10)))

(defn thanksgiving-us [year]
  (-> year
      november
      (nest :day)
      t-sequence
      (#(filter thursday? %))
      (nth 3)))

(fact "US Thanksgiving is 4th Thursday in November"
      (t-> "2017" thanksgiving-us) => "2017-11-23"
      (t-> "2018" thanksgiving-us) => "2018-11-22"
      (t->> "2017"
            (#(t-sequence {:starts %})) ;seq of years
            (map thanksgiving-us) ;seq of Thanksgivings
            (take 3)) => ["2017-11-23" "2018-11-22" "2019-11-28"])


(defn monday? [ymd]
  (-> ymd
      day-of-week
      (= 1)))

(defn may [year]
  (-> year
      (nest :month)
      t-sequence
      (nth 4)))

(defn memorial-day-us [year]
  (-> year
      may
      (nest :day)
      t-sequence
      (#(filter monday? %))
      last))

(fact "US Memorial Day is the last Monday in May"
      (t-> "2017" memorial-day-us) => "2017-05-29"
      (t-> "2018" memorial-day-us) => "2018-05-28")


;; Equivalent of dt plus period
;; razors edge
;;

(defn nth-or-last [rbi n]
  (
  )

(defn plus [t [scale n]]
            (-> t (enclosing scale)
                (#(t-sequence {:starts %}))
                (nth n)))

