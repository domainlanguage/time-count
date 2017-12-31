(ns time-count.explainer.b-counting-and-nesting
  (:require [time-count.core :refer :all]
            [time-count.iso8601 :refer [t-> t->>]]
            [time-count.metajoda]                           ;must have some implementation
            [midje.sweet :refer :all]))


;; As a reminder, we have two threading operations that allow us
;; to use the ISO 8601-based string representation in place of
;; the MetaJoda representation.

(fact "A CountableTime is part of a sequence, so next is meaningful"
      (t-> "2017-04-09" next-t) => "2017-04-10"
      (t-> "2017-04" next-t) => "2017-05"
      (t-> "2017" next-t) => "2018"
      (t-> "2017-04-09T11:17" next-t) => "2017-04-09T11:18"
      (t-> "2017-02-28" next-t) => "2017-03-01"
      (t-> "2016-02-28" next-t) => "2016-02-29"
      (t-> "2017-070" next-t) => "2017-071"
      (t-> "2017-365" next-t) => "2018-001"
      (t-> "2017-W52" next-t) => "2018-W01")


(facts "about nesting a sequence of CountableTimes within a CountableTime of a larger scale"

       (fact "A CountableTime sequence can be nested within a CountableTime."
             (t-> "2017" (nest :month) t-sequence)
             => ["2017-01" "2017-02" "2017-03" "2017-04" "2017-05" "2017-06"
                 "2017-07" "2017-08" "2017-09" "2017-10" "2017-11" "2017-12"])

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
             (t-> "2017-04-09" (enclosing :day)) => "2017-04-09"))


(fact "Nesting also defines an interval with a smaller scale"
      (t-> "2017" (nest :month)) => "2017-01/2017-12"
      (t-> "2017-12" (nest :day)) => "2017-12-01/2017-12-31")


(fact "An interval can be viewed as a sequence of times at the scale of the interval bounds."
      (t->> "2017-04/2017-06" t-sequence) => ["2017-04" "2017-05" "2017-06"]
      (t->> "2017-04/-" t-sequence (take 4)) => ["2017-04" "2017-05" "2017-06" "2017-07"]
      (t->> "-/2017-04" t-rev-sequence (take 4)) => ["2017-04" "2017-03" "2017-02" "2017-01"])



(fact "Some nestings can be mapped to each other, bidirectionally"
      (t-> "2017-04-25" (to-nesting [:day :week :week-year]))
      => "2017-W17-2"
      (t-> "2017-W17-2" (to-nesting [:day :month :year]))
      => "2017-04-25")

(fact "Most nestings don't map to each other"
      (t-> "2017-04" (to-nesting [:week :week-year]))
      => :no-mapping)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Using sequence operations and nesting to derive holidays ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn day-of-week [ymd]
  (-> ymd
      (to-nesting [:day :week :week-year])
      (place-value :day)))

(defn thursday? [ymd]
  (-> ymd
      day-of-week
      (= 4)))

(defn november [year]
  (-> year
      (nest :month)
      t-sequence
      (nth 10)))                                            ;sequence counts from zero!!

(defn thanksgiving-us [year]
  (-> year
      november
      (nest :day)
      t-sequence
      (#(filter thursday? %))
      (nth 3)))                                             ;sequence counts from zero!!

(fact "US Thanksgiving is 4th Thursday in November"
      (t-> "2017" thanksgiving-us) => "2017-11-23"
      (t-> "2018" thanksgiving-us) => "2018-11-22"
      (t->> "2017/-"
            t-sequence
            (map thanksgiving-us)                           ;sequence of Thanksgivings
            (take 3)) => ["2017-11-23" "2018-11-22" "2019-11-28"])


(defn monday? [ymd]
  (-> ymd
      day-of-week
      (= 1)))

(defn may [year]
  (-> year
      (nest :month)
      t-sequence
      (nth 4)))                                             ; sequence counts from 0

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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Alternative Derivation of the Same Values.
;; Because of the underlying model of nested sequences,
;; there are many equivalent ways to manipulate values
;; and derive the same results.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn place-value=
  "Return a predicate function."
  [scale value]
  (fn [t]
    (= value (place-value t scale))))

(def november? (place-value= :month 11))

(defn november-alt-derivation
  "In the first derivation, November was found using (nth 10)
  because November is the 11th month (and sequences are zero-based).
  Here is an equivalent method filtering on place-values."
  [year]
  (-> year
      (nest :month)
      t-sequence
      (#(filter november? %))
      first))                                               ; sequences are zero based!

(fact "November can be derived multiple ways."
      (t-> "2017" november) => "2017-11"
      (t-> "2017" november-alt-derivation) => "2017-11")


;; Rough equivalent of JodaTime's 'plus', dt plus period
;;  e.g. 2017-12-20 + P3D => 2017-12-23 (add 3 days)
;; However, it has some peculiar edge-cases
;;  This looks good: 2017-12-31 + P1M => 2018-01-31 (add 1 month)
;;  But this might surprise us:
;;  2018-01-31 + P1M => 2018-02-28
;;  There is a hidden rule, to use the last day-of-month
;;  (or hour-of-day, etc.) rather than failing or
;;  rolling over to the next month.
;;
;;  If this functionality is truly needed, it could
;;  be reproduced in time-count, and could be explicit.

(defn th
  "This isn't really time-count specific. It is just an
  alternative to nth, except it returns nil rather than
  throwing IndexOutOfBoundsException.
  And it counts the index from 1 rather than 0."
  [a-sequence n]
  (if (<= n (count a-sequence))
    (nth a-sequence (dec n))))


(defn nest-val-or-last
  "We are putting the default to use last here.
   It could have been put somewhere else.
   Note how natural it is to express JodaTime's
   default rule in terms of sequences."
  [t [place val]]
  (-> t
      (nest place) t-sequence
      (#(or (th % val) (last %)))))

(defn nest-vals-or-last
  "Nest multiple place-values (using the 'or-last' rule)."
  [t place-vals]
  (reduce nest-val-or-last t (reverse place-vals)))

(defn places-nested-in
  "Take the nested values out of their nesting."
  [t scale]
  (->> t
      place-values
      (take-while #(not= (first %) scale))))

(defn plus
  "Here the equivalent of a JodaTime period is a place-value
   for a certain scale (e.g. [:day 5] vs P5D)
   (Maybe an ISO string mapping for periods/place-values would be an interesting experiment.)
   This function can add only a one-scale period.
   Period with multiple scales could be made with reduce."
  [t scale increment]
    (-> t
        (enclosing scale)
        (#(t-sequence {:starts %}))
        (nth increment)
        (nest-vals-or-last (places-nested-in t scale))))

(fact "Place-value/periods can be added to times."
      (t-> "2017-12-20" (plus :day 3))
      => "2017-12-23"
      (t-> "2017-12-31" (plus :month 1))
      => "2018-01-31")

(fact "When a place-value range is exceeded in plus, use last in sequence."
      (t-> "2018-01-31" (plus :month 1))
      => "2018-02-28"
      (t-> "2018-01-31" (plus :month 3))
      => "2018-04-30")
