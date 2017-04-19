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
    :else :NONE)
  )

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

(fact "In cannonical string, least significant place is unit."
      (stringify [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])
      => "2017-01-10"
      (stringify [(DateTime. 2017 1 10 0 0 0 0) :month :year])
      => "2017-01")

(fact "Pattern can be recognized from string."
      (time-string-pattern "2017-02-13") => "yyyy-MM-dd"
      (time-string-pattern "2017-02") => "yyyy-MM"
      (time-string-pattern "2017-02-13T18:09") => "yyyy-MM-dd'T'HH:mm")

(fact "Parsing can be constrained to a specific pattern or left open."
  ((partial destringify "yyyy-MM") "2017-01")
  => [(DateTime. 2017 1 1 0 0 0 0) :month :year]
  ((partial destringify "yyyy-MM-dd") "2017-01-10")
  => [(DateTime. 2017 1 10 0 0 0 0) :day :month :year]
  (destringify "2017-01")
  => [(DateTime. 2017 1 1 0 0 0 0) :month :year]
  (destringify "2017-01-10")
  => [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])


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
  (= (stringify mj1) (stringify mj2)))


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

(future-fact "Daylight savings time"
             (-> "2016-11-06T01:59" destringify next-interval)
             => "?")
