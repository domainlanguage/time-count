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


