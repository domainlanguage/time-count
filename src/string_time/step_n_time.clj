(ns string-time.step-n-time
  (:require [string-time.meta-joda :refer :all])
  (:import [org.joda.time DateTime]))


(defn after?
  "TODO This is too simplistic."
  [[^DateTime dt1 & nesting1] [^DateTime dt2 & nesting2]]
  (.isAfter dt1 dt2))

(defn next-interval [[^DateTime dt & nesting]]
  (cons
    (.plus dt (scale-to-Period (first nesting)))
    nesting))

(defn interval-seq
  ([meta-joda-start]
   (iterate next-interval meta-joda-start))
  ([meta-joda-start meta-joda-end]
   (take-while #(not (after? % meta-joda-end)) (interval-seq meta-joda-start))))

(defn nested-first [scale]
  (fn [[^DateTime dt & nesting]]
    (cons (-> dt ((nesting-fns scale (first nesting))) .withMinimumValue)
          (cons scale nesting))))

(defn nested-last [scale]
  (fn [[^DateTime dt & nesting]]
    (cons (-> dt ((nesting-fns scale (first nesting))) .withMaximumValue)
          (cons scale nesting))))

(defn nested-seq
  "Return a lazy sequence of subintervals of the specified scale.
   If there is no nested scale in the schema, return empty seq(?)."
  [scale]
  (fn [meta-joda]
    (interval-seq ((nested-first scale) meta-joda)
                  ((nested-last scale) meta-joda))))

(defn- enclosing-interval
  ([[^DateTime date & nesting]]
   (cons date (rest nesting)))
  ([[^DateTime date & nesting] scale]
   (cons date (drop-while #(-> % (= scale) not) nesting))))

(defn enclosing
  ([] enclosing-interval)
  ([scale] (fn [jt] (enclosing-interval jt scale))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; composition of transformations and destring/stringify ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn t-> [& meta-joda-fns]
  (fn [time-string]
    (-> time-string
        destringify
        ((apply comp (reverse meta-joda-fns)))
        stringify)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;  Allen's Interval Algebra  ;;;
;;;  Basic relations           ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn allens-mj [[^DateTime dt-a & nesting-a :as a] [^DateTime dt-b & nesting-b :as b]]
  (let [a-left dt-a
        [a-right] (next-interval a)
        b-left dt-b
        [b-right] (next-interval b)]
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
    ))

(defn relation [a b]
  (allens-mj
    (destringify a)
    (destringify b)))