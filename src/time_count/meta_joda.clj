(ns time-count.meta-joda
  (:import [org.joda.time DateTime
                          Years Months Weeks Days Hours Minutes Seconds]
           [org.joda.time.format DateTimeFormat])
  (:require [midje.sweet :refer :all]
            [clojure.set :refer [map-invert]]))


(defn scale-to-Period [scale]
  (case scale
    :year (Years/years 1)
    :month (Months/months 1)
    :day (Days/days 1)
    :hour (Hours/hours 1)
    :week (Weeks/weeks 1)
    :minute (Minutes/minutes 1)
    :no-match))


(defn nesting-fns [enclosing-scale nested-scale]
  (case [enclosing-scale nested-scale]
    [:month :year] (fn [^DateTime dt] (.monthOfYear dt))
    [:day :month] (fn [^DateTime dt] (.dayOfMonth dt))
    [:hour :day] (fn [^DateTime dt] (.hourOfDay dt))
    [:minute :hour] (fn [^DateTime dt] (.minuteOfHour dt))
    [:day :year] (fn [^DateTime dt] (.dayOfYear dt))
    [:week :week-year] (fn [^DateTime dt] (.weekOfWeekyear dt))
    [:day :week] (fn [^DateTime dt] (.dayOfWeek dt))
    :no-match))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; String representations ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def pattern-to-nesting
  {"yyyy"               [:year]
   "yyyy-MM"            [:month :year]
   "yyyy-MM-dd"         [:day :month :year]
   "yyyy-MM-dd'T'HH"    [:hour :day :month :year]
   "yyyy-MM-dd'T'HH:mm" [:minute :hour :day :month :year]
   "yyyy-DDD"           [:day :year]
   "xxxx" [:week-year]
   "xxxx-'W'ww" [:week :week-year]
   "xxxx-'W'ww-e" [:day :week :week-year]})

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

(defn destringify
  ([pattern time-string]
    (cons
      (.parseDateTime (formatter-for-pattern pattern) time-string)
      (pattern-to-nesting pattern)))
  ([time-string]
    (destringify (time-string-pattern time-string) time-string)))


(defn destringifier-from-scales [nesting]
  (fn [time-string]
    (cons
      (.parseDateTime (formatter-for-nesting nesting) time-string)
      nesting)))

(defn stringify [[^DateTime date & nesting]]
  (.print (formatter-for-nesting nesting) date))



;;;;;;;;;;;;
;; Basics ;;
;;;;;;;;;;;;

(defn default-insignificant-scales
  "JodaDates encode all scales, but often some are ignored.
  For example, when we want to represent a date without a specific time.
  meta-Joda makes that explicit. For most operations, the values
  of insignificant scales (such as hour within a :day :month :year)
  are simply ignored. Although they *cannot* be removed,
  in some cases it may be useful to set them to a default.
  This function does that."
  [[_ & scales :as t]]
  ;TODO This is awfully indirect! Probably slow. Easy, for getting started.
  (-> t
      stringify
      ((destringifier-from-scales scales))))

;; TODO Seems like it might be better to be consistent: Either ignore insignificant scales or use this function whenever insignificant scales occur
(fact "Each scale has a default value -- typically its lowest.
              Ignored scales can be set to these values."
      (default-insignificant-scales [(DateTime. 2017 5 5 5 5 5 5) :day :month :year])
      => [(DateTime. 2017 5 5 0 0 0 0) :day :month :year]
      (default-insignificant-scales [(DateTime. 2017 5 5 5 5 5 5) :month :year])
      => [(DateTime. 2017 5 1 0 0 0 0) :month :year]
      (default-insignificant-scales [(DateTime. 2017 5 5 5 5 5 5) :minute :hour :day :month :year])
      => [(DateTime. 2017 5 5 5 5 0 0) :minute :hour :day :month :year])

(defn same-time? [mj1 mj2]
  ;; TODO This seems quite indirect. Is it inefficient?
  ;; TODO This function is only used in tests, and only because of insignificant scales. Allens "equals" is the real comparison. Somehow get rid of it?
  (= (stringify mj1) (stringify mj2)))

;; TODO Same point here about insignificant scales. Ignore them or eliminate them?
(fact "Nested scales of a named time are explicit.
       The value of any other scale is ignored"
      (same-time? [(DateTime. 2017 1 10 0 0 0 0) :day :month :year]
                  [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])
      => true
      (same-time? [(DateTime. 2017 1 10 0 0 0 0) :day :month :year]
                  [(DateTime. 2017 1 11 0 0 0 0) :day :month :year])
      => false
      (same-time? [(DateTime. 2017 1 10 0 0 0 0) :month :year]
                  [(DateTime. 2017 1 11 0 0 0 0) :month :year])
      => true
      (same-time? [(DateTime. 2017 1 10 0 0 0 0) :day :month :year]
                  [(DateTime. 2017 1 10 0 0 0 0) :month :year])
      => false)

(def mapable-nestings
  [[:day :month :year]
   [:day :year]
   [:day :week :week-year]])

(defn to-nesting
  ;;TODO Validate that both in and out are mapable-nestings
  ([target-nesting [^DateTime date & nesting]] (cons date target-nesting))
  ([target-nesting] (partial to-nesting target-nesting)))


(facts "about mapping between nesting"
       (fact ":day :month :year maps to :day :year"
         (-> "2017-04-25" destringify ((to-nesting [:day :week :week-year])) stringify)
             => "2017-W17-2"))   

(future-fact "Daylight savings time"
             (-> "2016-11-06T01:59" destringify next-interval)
             => "?")
