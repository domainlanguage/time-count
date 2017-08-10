(ns time-count.time-count
  (:require [time-count.meta-joda :refer :all])
  (:import [org.joda.time DateTime]))


(defn next-interval [[^DateTime dt & nesting]]
  (cons
    (.plus dt (scale-to-Period (first nesting)))
    nesting))

(defn- joda-after?
  "This is a convenience wrapper, used to create interval bounded sequences.
  After, in this method, is used in the sense of JodaTime, and NOT Allen's relation."
  [[^DateTime dt1 & nesting1] [^DateTime dt2 & nesting2]]
  (.isAfter dt1 dt2))

(defn interval-seq
  ([meta-joda-starts]
   (iterate next-interval meta-joda-starts))
  ([meta-joda-starts meta-joda-finishes]
   (take-while #(not (joda-after? % meta-joda-finishes)) (interval-seq meta-joda-starts))))

(defn interval-seq2
  [{:keys [starts finishes]}]
  (interval-seq starts finishes))

(defn nested-first
  ([scale]
   (fn [[^DateTime dt & nesting]]
     (cons (-> dt ((nesting-fns scale (first nesting))) .withMinimumValue)
           (cons scale nesting))))
  ([scale mjt]
   ((nested-first scale) mjt)))

(defn nested-last
  ([scale]
   (fn [[^DateTime dt & nesting]]
     (cons (-> dt ((nesting-fns scale (first nesting))) .withMaximumValue)
           (cons scale nesting))))
  ([scale mjt]
   ((nested-last scale) mjt)))


(defn nested-seq
  "Return a lazy sequence of subintervals of the specified scale.
   If there is no nested scale in the schema, return empty seq(?)."
  ([scale] (partial nested-seq scale))
  ([scale meta-joda]
   (interval-seq ((nested-first scale) meta-joda)
                 ((nested-last scale) meta-joda))))

;(defn enclosing
;  ([[^DateTime date & nesting]]
;   (cons date (rest nesting)))
;  ([scale [^DateTime date & nesting]]
;   (cons date (drop-while #(-> % (= scale) not) nesting))))

(defn enclosing-immediate
  ([[^DateTime date & nesting]]
   (cons date (rest nesting))))

(defn enclosing
  "The enclosing interval of a specific scale."
  ([scale] (partial enclosing scale))
  ([scale [^DateTime date & nesting]]
   (cons date (drop-while #(-> % (= scale) not) nesting))))
