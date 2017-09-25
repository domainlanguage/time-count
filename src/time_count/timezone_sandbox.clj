(ns time-count.timezone-sandbox
  ;(:require [time-count.iso8601 :refer [formatter-for-pattern]])
  (:import [org.joda.time DateTime Minutes LocalDateTime DateTimeZone]
           [org.joda.time.format DateTimeFormat]))


;(def isoFormat (-> "yyyy-MM-dd'T'HH:mm" formatter-for-pattern .withOffsetParsed))

;(def tzFormat (-> "yyyy-MM-dd'T'HH:mmZ" formatter-for-pattern .withOffsetParsed))

(comment "Working with time-zones and non-timezones"
         (defn basic-parse [t-string]
           (.parseDateTime isoFormat t-string))

         (defn no-tz-parse [t-string]
           (.parseDateTime isoFormat (str t-string "Z")))


         (def pZ (-> "yyyy-MM-dd'T'HH:mmZ"  DateTimeFormat/forPattern .withOffsetParsed))
         (def pz (-> "yyyy-MM-dd'T'HH:mmz"  DateTimeFormat/forPattern .withOffsetParsed))
         (.parseDateTime pz "2017-09-17T10:18PST")
         )
;; new LocalDateTime(timestamp.getTime()).toDateTime(DateTimeZone.UTC);

;; new LocalDateTime("1946-04-14", dtz)

;; http://stackoverflow.com/questions/19002978/in-joda-time-how-to-convert-time-zone-without-changing-time
;; http://stackoverflow.com/questions/5451152/how-to-handle-jodatime-illegal-instant-due-to-time-zone-offset-transition?answertab=votes#tab-top

; (= (LocalDateTime. "2017-03-04T10:14" DateTimeZone/UTC) (LocalDateTime. "2017-03-04T10:14"))
; => true

;(.toDateTime (LocalDateTime. "2017-03-04T10:14") DateTimeZone/UTC)

;(.getZone (.toDateTime (LocalDateTime. "2017-03-04T10:14") DateTimeZone/UTC))

;(.getZone (.toDateTime (LocalDateTime. "2017-03-04T10:14") (DateTimeZone/forID "US/Eastern")))

(def x (.toDateTime (LocalDateTime. "2017-03-04T10:14") (DateTimeZone/forID "US/Eastern")))
(.plus x (Minutes/minutes 5))
(.getZone (.plus x (Minutes/minutes 5)))
(def pZ (-> "yyyy-MM-dd'T'HH:mmZ"  DateTimeFormat/forPattern .withOffsetParsed))
(def pz (-> "yyyy-MM-dd'T'HH:mmz"  DateTimeFormat/forPattern .withOffsetParsed))
(.parseDateTime pz "2017-09-17T10:18PST")

;; JodaTime can do everything needed, and is consistent with ISO.
;; The following calculations illustrate tricky gaps in timezone concepts:

;(def nyc (DateTimeZone/forID))

(def z-den (DateTimeZone/forID "America/Denver"))

(comment "Why doesn't this work?"
   (.getOffset z-den))

