(ns time-count.metajoda
  (:require [time-count.core :refer :all] ;[SequenceTime next-interval Interval ->RelationBoundedInterval]]
            [time-count.iso8601 :refer :all]
            [time-count.meta-joda :refer [scale-to-Period]]
    ; [time-count.time-count :refer [nested-first nested-last]]
            [time-count.allens-interval-algebra :refer [relation-mj]])

  (:import [org.joda.time DateTime]
           [org.joda.time.format DateTimeFormat]))




;; TODO Replace meta-joda require with new stuff in core
;; TODO Replace time-count require with new stuff here.
;; TODO Replace allens-interval-algebra require with new stuff somewhere.
;; TODO Add namespaced keywords to core.

(def joda-Property-for-nesting
  "These Property objects give access to JodaTime's logic of how scales fit together."
  {[:month :year]     (fn [^DateTime dt] (.monthOfYear dt))
   [:day :month]      (fn [^DateTime dt] (.dayOfMonth dt))
   [:hour :day]       (fn [^DateTime dt] (.hourOfDay dt))
   [:minute :hour]    (fn [^DateTime dt] (.minuteOfHour dt))
   [:day :year]       (fn [^DateTime dt] (.dayOfYear dt))
   [:week :week-year] (fn [^DateTime dt] (.weekOfWeekyear dt))
   [:day :week]       (fn [^DateTime dt] (.dayOfWeek dt))
   [:year]            (fn [^DateTime dt] (.year dt))
   [:week-year]       (fn [^DateTime dt] (.weekyear dt))})


(defn- nested-first
  [{:keys [^DateTime dt nesting]} nested-scale]
  (let [nesting-fn (joda-Property-for-nesting [nested-scale (first nesting)])]
    {:dt      (-> dt nesting-fn .withMinimumValue)
     :nesting (cons nested-scale nesting)}))

(defn- nested-last
  [{:keys [^DateTime dt nesting]} nested-scale]
  (let [nesting-fn (joda-Property-for-nesting [nested-scale (first nesting)])]
    {:dt      (-> dt nesting-fn .withMaximumValue)
     :nesting (cons nested-scale nesting)}))


(defrecord MetaJodaTime [^DateTime dt nesting]

  SequenceTime

  (next-interval [t]
    (MetaJodaTime.
      (.plus dt (scale-to-Period (first nesting)))
      nesting))

  (prev-interval [t]
    (MetaJodaTime.
      (.minus dt (scale-to-Period (first nesting)))
      nesting))

  (nest [t scale]
    (let [{dts :dt n :nesting} (nested-first t scale)
          {dtf :dt} (nested-last t scale)]

    (->RelationBoundedInterval
      (MetaJodaTime. dts n)
      (MetaJodaTime. dtf n)
      )))

  (enclosing-immediate [t]
    ;Clearing insignificant places: JodaTime has a millisecond representation, whatever the scale in meta-joda.
    ;Mostly, this would be ignored, but the first operation below sets it to a default of the first millisecond
    ;within the interval.

    (let [{dt-with-insignificant-places-cleared :dt} (nested-first {:dt dt :nesting (rest nesting)} (first nesting))]
      (MetaJodaTime. dt-with-insignificant-places-cleared (rest nesting))))


  ISO8601Mappable

  (to-iso [t]
    (.print (-> nesting nesting-to-pattern DateTimeFormat/forPattern .withOffsetParsed) dt))
  )


(extend-type String
  ISO8601Pattern
  (iso-parser [pattern]
    (fn [time-string]
      (MetaJodaTime.
        (.parseDateTime (-> pattern DateTimeFormat/forPattern .withOffsetParsed) time-string)
        (pattern-to-nesting pattern)))))


(extend-type String
  ISO8601SequenceTime
  (from-iso-sequence-time [time-string]
    ((-> time-string time-string-pattern iso-parser) time-string)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;  Allen's Interval Algebra  ;;;
;;;  Basic relations           ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(extend-protocol Interval
  MetaJodaTime
  (relation [{dt-a :dt :as a} {dt-b :dt :as b}]
    (let [a-left dt-a
          {a-right :dt} (next-interval a)
          b-left dt-b
          {b-right :dt} (next-interval b)]
      (cond
        (and (.isEqual a-left b-left)
             (.isEqual a-right b-right)) :equal
        (.isBefore a-right b-left) :before
        (.isAfter a-left b-right) :after
        (.isEqual a-right b-left) :meets
        (.isEqual b-right a-left) :met-by
        (and (.isEqual a-left b-left)
             (.isBefore a-right b-right)) :starts
        (and (.isEqual a-left b-left)
             (.isAfter a-right b-right)) :started-by
        (and (.isEqual a-right b-right)
             (.isAfter a-left b-left)) :finishes
        (and (.isEqual a-right b-right)
             (.isBefore a-left b-left)) :finished-by
        (and (.isAfter a-left b-left)
             (.isBefore a-right b-right)) :during
        (and (.isBefore a-left b-left)
             (.isAfter a-right b-right)) :contains
        (and (.isBefore a-left b-left)
             (.isBefore a-right b-right)
             (.isAfter a-right b-left)) :overlaps
        (and (.isBefore b-left a-left)
             (.isBefore b-right a-right)
             (.isAfter b-right a-left)) :overlapped-by

        :else :TBD)
      )))
