(ns time-count.time-count
  (:require [time-count.meta-joda :refer :all])
  (:import [org.joda.time DateTime]))


(defn next-interval [[^DateTime dt & nesting]]
  (cons
    (.plus dt (scale-to-Period (first nesting)))
    nesting))

(defn- after?
  "This is a convenience wrapper, used to create interval bounded sequences."
  [[^DateTime dt1 & nesting1] [^DateTime dt2 & nesting2]]
  (.isAfter dt1 dt2))

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

;(defn enclosing
;  ([[^DateTime date & nesting]]
;   (cons date (rest nesting)))
;  ([scale [^DateTime date & nesting]]
;   (cons date (drop-while #(-> % (= scale) not) nesting))))

(defn enclosing-interval
  ([[^DateTime date & nesting]]
   (cons date (rest nesting)))
  ([scale [^DateTime date & nesting]]
   (cons date (drop-while #(-> % (= scale) not) nesting))))

(defn enclosing
  ([] enclosing-interval)
  ([scale] (partial enclosing-interval scale)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; composition of transformations and destring/stringify ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tf-> [& meta-joda-fns]
  (fn [time-string]
    (-> time-string
        destringify
        ((apply comp (reverse meta-joda-fns)))
        stringify)))

(defn t-> [time-string & meta-joda-fns]
  (-> time-string ((apply tf-> meta-joda-fns))))

