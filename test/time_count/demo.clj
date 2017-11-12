(ns time-count.demo
  (:require [time-count.core :refer :all]
            [time-count.iso8601 :refer [to-iso from-iso t-> t->>]]
            [time-count.metajoda :refer [->MetaJodaTime]]
            [midje.sweet :refer :all])
  (:import [org.joda.time LocalDateTime]))


(fact "Time representation needs metadata representing nested scale"
      (-> "2017-04-09" from-iso) => (->MetaJodaTime (LocalDateTime. 2017 4 9 0 0 0 0) [:day :month :year])
      (to-iso (->MetaJodaTime (LocalDateTime. 2017 4 9 0 0 0 0) [:day :month :year])) => "2017-04-09"
      (-> "2017-04-09T11:17" from-iso :nesting) => [:minute :hour :day :month :year]
      (-> "2017-04" from-iso :nesting) => [:month :year])


(fact "Two threading macros allows application of time transforming functions with ISO 8601 strings."
      ;; This may be mostly for tests and demos. Perhaps it will be used in some apps for data interchange.
      (t-> "2017-04-30" identity) => "2017-04-30"
      (t->> "2017-04-30" identity) => "2017-04-30")

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

(fact "Each UTC offset and timezone is a distinct sequence."
      (t-> "2017-04-09T11:17" next-interval) => "2017-04-09T11:18"
      (t-> "2017-04-09T11:17-04:00" next-interval) => "2017-04-09T11:18-04:00"
      (t-> "2017-04-09T11:17-04:00[America/New_York]" next-interval) => "2017-04-09T11:18-04:00[America/New_York]")

(fact "Daylight savings time shifts are part of some sequences."
      (t-> "2017-11-05T01:59-04:00[America/New_York]" next-interval) => "2017-11-05T01:00-05:00[America/New_York]"
      (t-> "2017-11-05T01-04:00[America/New_York]" next-interval) => "2017-11-05T01-05:00[America/New_York]")
      ;TODO Does this make sense? Or should timezone not be for dates?
      ;(t-> "2017-11-05T-04:00[America/New_York]" next-interval) => "2017-11-05T-05:00[America/New_York]"



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
       (fact "A function to count forward is built on sequence operators.
              It could be partially applied to create a more specialized fn,
              relevant in a particular business rule"

             (letfn [(later [t n] (-> {:starts t} t-sequence (nth n)))]
               (t-> "2017-04-19" (later 5)) => "2017-04-24"
               (t-> "2017-04" (later 5)) => "2017-09"
               (t-> "2017" (later 5)) => "2022"
               (let [later5 #(later % 5)]
                 (t-> "2017" later5) => "2022")))

       (fact "An equivalent function to count forward, written in a different stlye.
              This style emphasizes partial application, perhaps to pass the fn
              around as a rule or part of a rule."
             (letfn [(later [n] (fn [t] (-> {:starts t} t-sequence (nth n))))]
               (t-> "2017-04-19" ((later 5))) => "2017-04-24"
               (t-> "2017-04" ((later 5))) => "2017-09"
               (t-> "2017" ((later 5))) => "2022"
               (let [later5 (later 5)]
                 (t-> "2017" later5) => "2022")))

       (fact "Business rules commonly refer to the last day of month, quarter or year.
              By composing the nesting navigation operations, we can make that."
             (letfn [(last-day [t] (-> t (nest :day) t-sequence last))]
               (t-> "2017-04" last-day) => "2017-04-30"
               (t-> "2017" last-day) => "2017-365"
               (t-> "2017-W02" last-day) => "2017-W02-7"))

       (fact "Alternative implementation,
              taking advantage of nest producing relation-bounded-interval."
             (letfn [(last-day [t] (-> t (nest :day) :finishes))]
               (t-> "2017-04" last-day) => "2017-04-30"
               (t-> "2017" last-day) => "2017-365"
               (t-> "2017-W02" last-day) => "2017-W02-7"))

       (fact "End-of-month is a common business operation, composed of seq and nest ops."
             (letfn [(eom [t] (-> t (enclosing :month) (nest :day) :finishes))]
               (t-> "2017-04-19" eom) => "2017-04-30"
               (t-> "2017-04" eom) => "2017-04-30"
               (t-> "2017-04-19T15:12" eom) => "2017-04-30"))
       ; A more complex eom could preserve nesting
       ; and find last interval of same scale, etc.
\
       (fact "The operations we create can be composed into higher ones also.
              E.g. End-of-month (eom) is a compositon of last-day and some nesting ops.
              A project might create their own library based on their business rules."
             (letfn [(last-day [t] (-> t (nest :day) t-sequence last))
                     (eom [t] (-> t (enclosing :month) last-day))]
               (t-> "2017-04-19" eom) => "2017-04-30"
               (t-> "2017-04" eom) => "2017-04-30"
               (t-> "2017-04-19T15:12" eom) => "2017-04-30"))

       (fact "Example: invoice due"
             (letfn [(overdue? [t due-date] (-> (relation t due-date) #{:after :met-by}))
                     (later [t n] (-> {:starts t} t-sequence (nth n)))
                     (last-day [t] (-> t (nest :day) t-sequence last))
                     (eom [t] (-> t (enclosing :month) last-day))
                     (net-30 [t] (-> t (enclosing :day) (later 30)))]
               (t-> "2017-01-15" net-30) => "2017-02-14"

               (let [work-completion (from-iso "2017-01-15T17:00")
                     t1 (from-iso "2017-02-10T11:10")
                     t2 (from-iso "2017-02-20T14:30")
                     t3 (from-iso "2017-03-01T09:15")]

                 (overdue? t1 (net-30 work-completion)) => falsey
                 (overdue? t2 (net-30 work-completion)) => truthy
                 (overdue? t3 (net-30 work-completion)) => truthy

                 (let [net-30-eom (comp eom net-30)] ;compose different payment terms
                   (t-> "2017-01-15T17:00" net-30-eom) => "2017-02-28"
                   (overdue? t1 (net-30-eom work-completion)) => falsey
                   (overdue? t2 (net-30-eom work-completion)) => falsey
                   (overdue? t3 (net-30-eom work-completion)) => truthy)))))


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
            (#(t-sequence {:starts %}))                     ;seq of years
            (map thanksgiving-us)                           ;seq of Thanksgivings
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


;; Rough equivalent of JodaTime's 'plus', dt plus period
;; work in progress

;(defn nth-or-last [rbi n]
;  nil
;  )

;(fact "If there is an nth, it is returned"
;      (t-> "2017-09" (nest :day) (nth-or-last 5)) => "2017-09-05"

;(defn plus [t [scale n]]
;  (-> t (enclosing scale)
;      (#(t-sequence {:starts %}))
;      (nth n)))

