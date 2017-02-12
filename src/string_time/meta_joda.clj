(ns string-time.meta-joda
  (:import [org.joda.time DateTime
                          Years Months Weeks Days Hours Minutes Seconds]
           [org.joda.time.format DateTimeFormat])
  (:require [midje.sweet :refer :all]
            [clojure.set :refer [map-invert]]))


(defn scale-to-Period [scale]
  (case scale
    :year (Years/years 1)
    :month (Months/months 1)
    :weeks (Weeks/weeks 1)
    :day (Days/days 1)
    :hour (Hours/hours 1)
    :no-match))


(defn nesting-fns [enclosing-scale nested-scale]
  (case [enclosing-scale nested-scale]
    [:month :year] (fn [^DateTime dt] (.monthOfYear dt))
    [:day :month] (fn [^DateTime dt] (.dayOfMonth dt))
    [:hour :day] (fn [^DateTime dt] (.hourOfDay dt))
    [:minute :hour] (fn [^DateTime dt] (.hourOfDay dt))
    [:day :year] (fn [^DateTime dt] (.dayOfYear dt))
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
   "yyyy-DDD"           [:day :year]})

(def nesting-to-pattern
  (map-invert pattern-to-nesting))

(defn formatter-for-pattern [pattern] (DateTimeFormat/forPattern pattern))

(defn formatter-for-nesting [nesting] (-> nesting nesting-to-pattern formatter-for-pattern))


(defn destringifier [pattern]
  (fn [date-string]
    (cons
      (.parseDateTime (DateTimeFormat/forPattern pattern) date-string)
      (pattern-to-nesting pattern))))

(defn destringifier-from-scales [nesting]
  (fn [date-string]
    (cons
      (.parseDateTime (formatter-for-nesting nesting) date-string)
      nesting)))

(defn stringify [[^DateTime date & nesting]]
  (.print (formatter-for-nesting nesting) date))

(fact "In cannonical string, least significant place is unit."
      (stringify [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])
      => "2017-01-10"
      (stringify [(DateTime. 2017 1 10 0 0 0 0) :month :year])
      => "2017-01")

(fact
  ((destringifier "yyyy-MM") "2017-01")
  => [(DateTime. 2017 1 1 0 0 0 0) :month :year]
  ((destringifier "yyyy-MM-dd") "2017-01-10")
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