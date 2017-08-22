(ns time-count.iso8601
  (:require [time-count.core :refer [map->RelationBoundedInterval]]
            [clojure.set :refer [map-invert]])
  ;(:import [time_count.core SequenceTime])
  (:import (time_count.core RelationBoundedInterval)))

;; String representation of time values is needed for
;;  - data interchange
;;  - tests
;;  - demos
;;  - ...
;;
;; The chosen string representation of time-count is ISO 8601,
;; which is well-documented.
;;  Best reference: https://xkcd.com/1179/
;;  Good introductory explanation: https://en.wikipedia.org/wiki/ISO_8601
;;  The official source: https://www.iso.org/iso-8601-date-and-time-format.html

(defn time-string-pattern [time-string]
  (cond
    (re-matches #"\d\d\d\d" time-string) "yyyy"
    (re-matches #"\d\d\d\d-\d\d" time-string) "yyyy-MM"
    (re-matches #"\d\d\d\d-\d\d-\d\d" time-string) "yyyy-MM-dd"
    (re-matches #"\d\d\d\d-\d\d-\d\dT\d\d" time-string) "yyyy-MM-dd'T'HH"
    (re-matches #"\d\d\d\d-\d\d-\d\dT\d\d:\d\d" time-string) "yyyy-MM-dd'T'HH:mm"
    (re-matches #"\d\d\d\d-\d\d\d" time-string) "yyyy-DDD"
    (re-matches #"\d\d\d\d-W\d\d" time-string) "xxxx-'W'ww"
    (re-matches #"\d\d\d\d-W\d\d-\d" time-string) "xxxx-'W'ww-e"
    :else :NONE))

(defprotocol ISO8601Mappable
  (to-iso [t] "ISO 8601 string representation of the SequenceTime or other interval."))

(defprotocol ISO8601Pattern
  (iso-parser [p] "Given an ISO 8601 pattern (as a string), return a function parses a timestring accordingly."))

(defprotocol ISO8601SequenceTime
  (from-iso-sequence-time [s] "Infer the pattern and parse the string accordingly into a SequenceTime"))

(extend-protocol ISO8601Mappable
  RelationBoundedInterval
  (to-iso [{:keys [starts finishes]}] (str (if starts (to-iso starts) "-") "/" (if finishes (to-iso finishes) "-"))))


(defn from-iso-to-relation-bounded-interval
  [interval-string]
  (let [[iso-starts iso-finishes] (clojure.string/split interval-string #"/")]

    (-> (#(if (not= "-" iso-starts)
            {:starts (from-iso-sequence-time iso-starts)}
            {}))
        (#(if (not= "-" iso-finishes)
            (assoc % :finishes (from-iso-sequence-time iso-finishes))
            %))
        map->RelationBoundedInterval)))

(defn from-iso [iso-string]
  (if (clojure.string/includes? iso-string "/")
    (from-iso-to-relation-bounded-interval iso-string)
    (from-iso-sequence-time iso-string)))



(def pattern-to-nesting
  {"yyyy"               [:year]
   "yyyy-MM"            [:month :year]
   "yyyy-MM-dd"         [:day :month :year]
   "yyyy-MM-dd'T'HH"    [:hour :day :month :year]
   "yyyy-MM-dd'T'HH:mm" [:minute :hour :day :month :year]
   "yyyy-DDD"           [:day :year]
   "xxxx"               [:week-year]
   "xxxx-'W'ww"         [:week :week-year]
   "xxxx-'W'ww-e"       [:day :week :week-year]})

(def nesting-to-pattern
  (map-invert pattern-to-nesting))

(defn stringify
  "Results of a time computation could be:
 - meta-joda value
 - a list of values
 - a relation-bounded-interval
 - other
 Each of the recognised types is converted into
 their equivalent string. Unrecognized types are
 returned unchanged.

 'destring' exactly inverts these conversions."
  [t]
  (cond
    (satisfies? ISO8601Mappable t) (to-iso t)
    (sequential? t) (map to-iso t)
    :else t))

(defn destring
  "Convert iso-strings or sequences of iso-strings.
  Not recursive. Doesn't allow lists of lists of
  iso-strings, for example."
  [iso-string-s]
  (cond
    (sequential? iso-string-s) (map from-iso iso-string-s)
    :else (from-iso iso-string-s)))


(defmacro t->
  "Pass in an iso-8601 string, or sequence, and some
  functions that operate on meta-joda times.
  Threads similar to ->, except with conversions
  before and after.
  Example (t-> \"2017\" time-count.time-count/interval-seq second)
  (Code is modified from ->> macro.)"
  [x & forms-in]
  (let [forms (concat [destring] forms-in [stringify])]
    (loop [x x, forms forms]
      (if forms
        (let [form (first forms)
              threaded (if (seq? form)
                         (with-meta `(~(first form) ~x ~@(next form)) (meta form))
                         (list form x))]
          (recur threaded (next forms)))
        x))))

(defmacro t->>
  "Like ->> for time. Pass in an iso-8601 string or sequence,
  and some functions that operate on meta-joda times.
  Threads similar to ->>, except with conversions
  before and after.
  Example (t->> \"2017\" time-count.time-count/interval-seq (take 3))
  (Code is modified from ->> macro.)"
  [x & in-forms]
  (let [forms (concat [destring] in-forms [stringify])]
    (loop [x x, forms forms]
      (if forms
        (let [form (first forms)
              threaded (if (seq? form)
                         (with-meta `(~(first form) ~@(next form) ~x) (meta form))
                         (list form x))]
          (recur threaded (next forms)))
        x))))