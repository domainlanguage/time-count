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
    {:starts   (iso-to-mj iso-starts)
     :finishes (iso-to-mj iso-finishes)}))

(defn relation-bounded-interval-to-iso
  [{:keys [starts finishes]}]
  (str (mj-to-iso starts) "/" (mj-to-iso finishes)))

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

(defn t-> [time-string & meta-joda-fns]
  (-> time-string
      iso-to-mj
      ((apply comp (reverse meta-joda-fns)))
      mj-to-iso))

