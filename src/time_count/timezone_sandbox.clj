(ns time-count.timezone-sandbox
 ; (:require [time-count.iso8601-old :refer [formatter-for-pattern]])
  (:import [org.joda.time DateTime Minutes LocalDateTime DateTimeZone]
           [org.joda.time.format DateTimeFormat]))


;(def isoFormat (-> "yyyy-MM-dd'T'HH:mm" formatter-for-pattern .withOffsetParsed))

;(def tzFormat (-> "yyyy-MM-dd'T'HH:mmZ" formatter-for-pattern .withOffsetParsed))

(comment "Working with time-zones and non-timezones"
         (defn basic-parse [t-string]
           (.parseDateTime isoFormat t-string))

         (defn no-tz-parse [t-string]
           (.parseDateTime isoFormat (str t-string "Z")))
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
