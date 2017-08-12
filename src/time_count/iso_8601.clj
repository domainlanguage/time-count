(ns time-count.iso-8601
  (:import [org.joda.time.format DateTimeFormat])
  (:require [time-count.meta-joda :refer [mj-time?]]
    [clojure.set :refer [map-invert]]
            [clojure.string :refer [split]]))

;; String representation of time values is important
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

(defn formatter-for-pattern [pattern] (-> pattern DateTimeFormat/forPattern .withOffsetParsed))

(defn formatter-for-nesting [nesting] (-> nesting nesting-to-pattern formatter-for-pattern))

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

(defn iso-to-mj
  ([pattern time-string]
   (cons
     (.parseDateTime (formatter-for-pattern pattern) time-string)
     (pattern-to-nesting pattern)))
  ([time-string]
   (iso-to-mj (time-string-pattern time-string) time-string)))


(defn destringifier-from-scales [nesting]
  (fn [time-string]
    (cons
      (.parseDateTime (formatter-for-nesting nesting) time-string)
      nesting)))

(defn mj-to-iso [[^DateTime date & nesting]]
  (.print (formatter-for-nesting nesting) date))

(defn iso-to-relation-bounded-interval
  [interval-string]
  (let [[iso-starts iso-finishes] (split interval-string #"/")]

    (-> (#(if (not= "-" iso-starts)
            {:starts (iso-to-mj iso-starts)}
           {}))
        (#(if (not= "-" iso-finishes)
            (assoc % :finishes (iso-to-mj iso-finishes))
            %)))))

(defn relation-bounded-interval-to-iso
  [{:keys [starts finishes]}]
  (str (if starts (mj-to-iso starts) "-") "/" (if finishes (mj-to-iso finishes) "-")))

(defn from-iso [iso-string]
  (if (clojure.string/includes? iso-string "/")
    (iso-to-relation-bounded-interval iso-string)
    (iso-to-mj iso-string)))

(defn to-iso [interval]
  (if (mj-time? interval)
    (mj-to-iso interval)
    (relation-bounded-interval-to-iso interval)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; composition of transformations and destring/stringify ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;(defn tf-> [& meta-joda-fns]
;  (fn [time-string]
;    (-> time-string
;        destringify
;        ((apply comp (reverse meta-joda-fns)))
;        stringify)))

(defn- destring
  "Turn possible inputs into meta-joda or other
  usable forms. See details in 'stringify' comment.
  (Not sure unrecognized inputs should ever happen,
   but this keeps the symmetry with stringify."
  [possibly-time-string-s]
  (cond
    (sequential? possibly-time-string-s) (map destring possibly-time-string-s)
    (string? possibly-time-string-s) (iso-to-mj possibly-time-string-s)
    :else possibly-time-string-s))

(defn- stringify

  "Results of a time computation could be:
   - meta-joda value
   - a list of values
   - a relation-bounded-interval
   - other
   Each of the recognised types is converted into
   their equivalent string. Unrecognized types are
   returned unchanged.

   'destring' exactly inverts these conversions."
  [possibly-mjt-s]
  (cond
    (mj-time? possibly-mjt-s) (mj-to-iso possibly-mjt-s)
    (sequential? possibly-mjt-s) (map stringify possibly-mjt-s)
    :else possibly-mjt-s))

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
                         (with-meta `(~(first form) ~@(next form)  ~x) (meta form))
                         (list form x))]
          (recur threaded (next forms)))
        x))))