(ns time-count.transform-sandbox
  (:import [org.joda.time ReadablePartial ReadableDateTime ReadableInstant
                          ReadablePeriod DateTime DateMidnight YearMonth
                          LocalDate LocalTime DateTimeZone Period PeriodType
                          Interval Years Months Weeks Days Hours Minutes Seconds
                          LocalDateTime MutableDateTime DateTimeUtils]
           [org.joda.time.base BaseDateTime]
           [org.joda.time.format DateTimeFormat])
  (:require [midje.sweet :refer :all]))



;; NOTICE: This namespace is not really part of the project
;; It is not source, test, or even example
;; It's purpose it to make it convenient to do experiments.
;; (For example, lots of stuff is imported from other namespaces
;; for convenience, because minimizing dependencies doesn't matter here.)
;; As such, perhaps it should be removed from the repo.
;; For now, it remains here because the PURPOSE of time-count
;; is to explore and illustrate ideas.
;; So, don't expect this namespace to be tidy or even correct.
;; But maybe you'll want to fiddle around in it.


;(defn days-in-month [^DateTime date]
;  (while)
;  )

(defn next-day [^DateTime date]
  (.plus date (Days/days 1)))

(fact
  (next-day (DateTime. 2017 1 2 0 0 0 0)) => (DateTime. 2017 1 3 0 0 0 0))

(defn next-month [^DateTime date]
  (.plus date (Months/months 1)))

(defn days-starting-from [^DateTime date]
  (iterate next-day date))

(defn last-day-of-month [^DateTime date]
  (let [in-month? (fn [^DateTime d]
                    (-> d .monthOfYear .get (= (-> date .monthOfYear .get))))]
    (->> (days-starting-from date)
         (take-while in-month?)
         last)))

(defn last-day-of-month2 [^DateTime date]
  (-> date .dayOfMonth .withMaximumValue))

;;;

(defn subintervals [containing-unit sub-unit]
  (case [containing-unit sub-unit]
    [:month :year] (fn [^DateTime date] (.monthOfYear date))
    [:day :month] (fn [^DateTime date] (.dayOfMonth date))
    [:hour :day] (fn [^DateTime date] (.hourOfDay date))
    :NONEXISTENT
    ))

(defn unit-to-Period [unit]
  (case unit
    :year (Years/years 1)
    :month (Months/months 1)
    :day (Days/days 1)
    :hour (Hours/hours 1)
    :NONEXISTENT))

(def nesting-levels
  [[:second :minute :hour :day :month :year]
   [:second :minute :hour :day :year]
   [:second :minute :hour :day :week :year]])


; chronology provides various sequences of intervals
; years year-month year-month-day year-day ...
; Also, days of month is a finite sequence. months in year...
; For the finite sequences, we can iterate the lazy sequence, or get first or last.
; (first might be from the sequence, but probably a separate op, symetrical with last.


(defn enclosing-interval
  ([[^DateTime date & units]]
   (cons date (rest units)))
  ([[^DateTime date & units] unit]
   (cons date (drop-while #(-> % (= unit) not) units))))

(defn first-sub-interval [[^DateTime date & units] unit]
  (cons (-> date ((subintervals unit (first units))) .withMinimumValue)
        (cons unit units)))

(defn last-sub-interval [[^DateTime date & units] unit]
  (cons (-> date ((subintervals unit (first units))) .withMaximumValue)
        (cons unit units)))


(defn last-enclosed [unit]
  #(last-sub-interval % unit))

(defn next-t [[^DateTime date & units]]
  (cons
    (.plus date (unit-to-Period (first units)))
    units))

(defn- after?
  [[^DateTime date1 & units1] [^DateTime date2 & units2]]
  (.isAfter date1 date2))

(defn seq-of
  "Return a lazy sequence of subintervals of the specified scale.
  If there is no nested scale in the schema, return empty seq."
  ([unit meta-joda]
   (let [max-t (last-sub-interval meta-joda unit)]
     (take-while #(not (after? % max-t)) (iterate next-t (first-sub-interval meta-joda unit)))
     ))
  ([unit]
   (partial seq-of unit)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; String representations ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def formatters
  {[:year]                           (DateTimeFormat/forPattern "yyyy")
   [:month :year]                    (DateTimeFormat/forPattern "yyyy-MM")
   [:day :month :year]               (DateTimeFormat/forPattern "yyyy-MM-dd")
   [:hour :day :month :year]         (DateTimeFormat/forPattern "yyyy-MM-dd'T'HH")
   [:minute :hour :day :month :year] (DateTimeFormat/forPattern "yyyy-MM-dd'T'HH:mm")})

(def pattern-elements
  {"yyyy-MM"    [:month :year]
   "yyyy-MM-dd" [:day :month :year]})

(defn destringifier [pattern]
  (fn [date-string]
    (cons
      (.parseDateTime (DateTimeFormat/forPattern pattern) date-string)
      (pattern-elements pattern))))

(defn stringify [[^DateTime date & units]]
  (.print (formatters units) date))

(fact "In cannonical string, least significant place is unit."
      (stringify [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])
      => "2017-01-10"
      (stringify [(DateTime. 2017 1 10 0 0 0 0) :month :year])
      => "2017-01")

(fact
  ((destringifier "yyyy-MM") "2017-01")
  => [(DateTime. 2017 1 1 0 0 0 0) :month :year]
  ((destringifier "yyyy-MM-dd") "2017-01-10")
  => [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])

(fact
  ;  (-> [(DateTime. 2017 1 10 0 0 0 0) :year :month :day]
  ;      (containing-interval :month))
  ;  => [(DateTime. 2017 1 31 0 0 0 0) :year :month]

  (-> [(DateTime. 2017 1 10 0 0 0 0) :day :month :year]
      (enclosing-interval :month)
      stringify)
  => "2017-01"

  (-> [(DateTime. 2017 1 10 0 0 0 0) :day :month :year]
      (enclosing-interval :month)
      (last-sub-interval :day))
  => [(DateTime. 2017 1 31 0 0 0 0) :day :month :year]

  (-> [(DateTime. 2017 1 10 0 0 0 0) :day :month :year]
      (enclosing-interval :month)
      (first-sub-interval :day))
  => [(DateTime. 2017 1 1 0 0 0 0) :day :month :year])

(fact
  (-> [(DateTime. 2017 1 10 0 0 0 0) :day :month :year]
      next-t)
  => [(DateTime. 2017 1 11 0 0 0 0) :day :month :year]

  ;  (-> [(DateTime. 2017 1 10 0 0 0 0) :day :month :year]
  ;      (containing-interval :month)
  ;      next-t)
  ;  => [(DateTime. 2017 2 1 0 0 0 0) :month :year]
  (-> [(DateTime. 2017 1 10 0 0 0 0) :day :month :year]
      (enclosing-interval :month)
      next-t
      stringify)
  => "2017-02")

(defn next-month-str1 [month-string]
  (-> month-string
      ((destringifier "yyyy-MM-dd"))
      (#(enclosing-interval % :month))
      next-t
      stringify))

(def next-month-str2
  (comp
    stringify
    next-t
    #(enclosing-interval % :month)
    (destringifier "yyyy-MM-dd")))

(defn date-transform [pattern & transformations]
  (apply comp (reverse
                (concat [(destringifier pattern)]
                        transformations
                        [stringify]))))

(def next-month-str3
  (date-transform "yyyy-MM-dd"
                  enclosing-interval
                  next-t))

(def end-of-next-month
  (date-transform "yyyy-MM-dd"
                  enclosing-interval
                  next-t
                  (last-enclosed :day)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Now more about an enclosing-immediate interval as a series ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment "Stuff to execute in REPL"

; day 6 of 2017-01 = 2017-01-06

(defn as-head-of-seq [meta-joda]
  (iterate next-t meta-joda))


(defn nth-of
  "Here's a versatile but potentially slow implementation."
  [match-rule unit n [^DateTime date & units :as meta-joda]]
  ;match-rule is :exact or :nearest only :exact for now.
  (first-sub-interval meta-joda unit)
  ;iterate through the subintervals, testing each for :day 6
  ; and return that one or :UNDEFINED
  )

(fact "A 'month' can be represented as a series of 'days'."
      (-> [(DateTime. 2017 1 1 0 0 0 0) :month :year]
          ((seq-of :day))
          count)
      => 31

      (-> [(DateTime. 2017 1 1 0 0 0 0) :month :year]
          ((seq-of :day))
          first)
      => [(DateTime. 2017 1 1 0 0 0 0) :day :month :year]

      (-> [(DateTime. 2017 1 1 0 0 0 0) :month :year]
          ((seq-of :day))
          last)
      => [(DateTime. 2017 1 31 0 0 0 0) :day :month :year]

      (-> [(DateTime. 2017 1 1 0 0 0 0) :month :year]
          ((seq-of :day))
          (nth 0))
      => [(DateTime. 2017 1 1 0 0 0 0) :day :month :year]

      (-> [(DateTime. 2017 1 1 0 0 0 0) :month :year]
          ((seq-of :day))
          (nth 6))
      => [(DateTime. 2017 1 7 0 0 0 0) :day :month :year])

(future-fact "Value of a particular scale can be extracted"
             ((val-of :day) [(DateTime. 2017 4 15 0 0 0 0) :day :month :year])
             => 15
             ((val-of :month) [(DateTime. 2017 4 15 0 0 0 0) :day :month :year])
             => 4)

(future-fact
  (nth-of :exact :day 6 [(DateTime. 2017 1 1 0 0 0 0) :month :year])
  => [(DateTime. 2017 1 6 0 0 0 0) :day :month :year])

(future-fact
  (of [:day 31] [(DateTime. 2017 1 1 0 0 0 0) :month :year])
  => [(DateTime. 2017 1 31 0 0 0 0) :day :month :year]
  (of [:day 32] [(DateTime. 2017 1 1 0 0 0 0) :month :year])
  => :NONEXISTENT)

; [a :year :month :day]
; (-> a month next last-day)
; ((comp last-day next month) a)
; (month a) => [a :year :month] => b
; (next b) => ((next :month) b) =>


; a date + a day = next day
; a date + an hour = undefined
; a date + a month = same day in next year
;   (ambiguous cases, eg March 31 + 1 month = ?
; ok for regular cycles (weeks, days, hours, minutes, milliseconds)
; month within year is regular cycle.

)