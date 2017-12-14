(ns time-count.joda-timezone-sandbox
  (:require [time-count.iso8601 :refer :all]
            [time-count.metajoda :refer :all]
            [time-count.core :refer :all])
  (:import [org.joda.time DateTime Minutes LocalDateTime DateTimeZone]
           [org.joda.time.format DateTimeFormat ISODateTimeFormat ISOPeriodFormat]))


;(def isoFormat (-> "yyyy-MM-dd'T'HH:mm" formatter-for-pattern .withOffsetParsed))

;(def tzFormat (-> "yyyy-MM-dd'T'HH:mmZ" formatter-for-pattern .withOffsetParsed))

(comment "Working with time-zones and non-timezones"
         (defn basic-parse [t-string]
           (.parseDateTime isoFormat t-string))

         (defn no-tz-parse [t-string]
           (.parseDateTime isoFormat (str t-string "Z")))


         (def pZ (-> "yyyy-MM-dd'T'HH:mmZ" DateTimeFormat/forPattern .withOffsetParsed))
         (def pz (-> "yyyy-MM-dd'T'HH:mmz" DateTimeFormat/forPattern .withOffsetParsed))
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
(def pZ (-> "yyyy-MM-dd'T'HH:mmZ" DateTimeFormat/forPattern .withOffsetParsed))
(def pz (-> "yyyy-MM-dd'T'HH:mmz" DateTimeFormat/forPattern .withOffsetParsed))
(.parseDateTime pz "2017-09-17T10:18PST")


;; JodaTime can do everything needed, and is consistent with ISO.
;; The following calculations illustrate tricky gaps in timezone concepts:

;(def nyc (DateTimeZone/forID))

(def z-den (DateTimeZone/forID "America/Denver"))

(comment "Why doesn't this work? (Oh! Because of daylight savings time!)"
         (.getOffset z-den))


;; We are missing some language

; UTC
(def tZ (.parseDateTime pZ "2017-10-09T22:00Z"))

; UTC Offsets
(def tUTC-5 (.parseDateTime pZ "2017-10-09T22:00-05"))

; Regional TimeZone (what I'd call "local time"
(def t-Denver (.toDateTime tZ z-den))
; NOTE: The line above isn't quite right, but it makes the right type.
; getZone returns the expected values.
; Their types are different, which I suppose makes sense.
(.getZone tZ)                                               ;=> org.joda.time.UTCDateTimeZone "UTC"
(.getZone tUTC-5)                                           ;=> org.joda.time.tz.FixedDateTimeZone "-05:00"
(.getZone t-Denver)                                         ;=> org.joda.time.tz.CachedDateTimeZone "America/Denver"

;;Lack of a string representation for the regional time hints at the problem!
; That is, for offsets, ISO gives us "2017-10-09T22:00-05",
; but the Denver time has no clear string mapping.
; FOR NOW I'll use "2017-10-09T22:00-05[America/Denver]"

; Note Oct 14: Hey! It turns out that is the exact format java.time uses!

(def localT (LocalDateTime. "2017-03-04T10:14"))
(def offsetT (.toDateTime localT (DateTimeZone/forOffsetHours -4)))
;(.getZone offsetT)
;=> #object[org.joda.time.tz.FixedDateTimeZone 0x60ed876c "-04:00"]
;
(def nycT (.toDateTime localT (DateTimeZone/forID "America/New_York")))
;(.getZone nycT)
;=> #object[org.joda.time.tz.FixedDateTimeZone 0x60ed876c "-04:00"]

(-> "2017-03-04T10:14" LocalDateTime. type)
;=> org.joda.time.LocalDateTime

(-> "2017-11-05T00:59" LocalDateTime.
    (.toDateTime (DateTimeZone/forID "America/New_York")))
;=> #object[org.joda.time.DateTime 0x117b96ad "2017-11-05T00:59:00.000-04:00"]

(-> "2017-11-05T02:01" LocalDateTime.
    (.toDateTime (DateTimeZone/forID "America/New_York")))
;=> #object[org.joda.time.DateTime 0x4e39b647 "2017-11-05T02:01:00.000-05:00"]

(-> "2017-11-05T01:30" LocalDateTime.
    (.toDateTime (DateTimeZone/forID "America/New_York")))
;=> #object[org.joda.time.DateTime 0x6b14330c "2017-11-05T01:30:00.000-04:00"]

(-> "2017-11-05T01:30" LocalDateTime.
    (.toDateTime (DateTimeZone/forID "America/New_York"))
    (.plus (Minutes/minutes 60)))
;=> #object[org.joda.time.DateTime 0x7afdece4 "2017-11-05T01:30:00.000-05:00"]

(-> "2017-11-05T01:30" LocalDateTime.
    (.toDateTime (DateTimeZone/forOffsetHours -4))
    (.plus (Minutes/minutes 60)))
;=> #object[org.joda.time.DateTime 0x5f4c0579 "2017-11-05T02:30:00.000-04:00"]

(-> "2017-11-05T01:30" LocalDateTime.
    (.toDateTime (DateTimeZone/forOffsetHours -4))
    (.toDateTime (DateTimeZone/forID "America/New_York"))
    (.plus (Minutes/minutes 60)))
;=> #object[org.joda.time.DateTime 0x6d3f61c2 "2017-11-05T01:30:00.000-05:00"]

(-> "2017-11-05T01:30" LocalDateTime.
    (.toDateTime (DateTimeZone/forOffsetHours -5))
    (.toDateTime (DateTimeZone/forID "America/New_York"))
    (.plus (Minutes/minutes 60)))
;=> #object[org.joda.time.DateTime 0x6510c401 "2017-11-05T02:30:00.000-05:00"]

; ((-> time-string time-string-pattern iso-parser) time-string)
; (.parseLocalDateTime (-> pattern DateTimeFormat/forPattern .withOffsetParsed) time-string)

(comment parsing-times-with-offset
         (def time-string "2017-11-05T04:20")
         ;=> #'sandboxes.joda-timezone-sandbox/time-string
         (def pattern "yyyy-MM-dd'T'HH:mm")
         ;=> #'sandboxes.joda-timezone-sandbox/pattern
         (.parseLocalDateTime (-> pattern DateTimeFormat/forPattern .withOffsetParsed) time-string)
         ;=> #object[org.joda.time.LocalDateTime 0x6eef815d "2017-11-05T04:20:00.000"]
         (.parseDateTime (-> pattern DateTimeFormat/forPattern .withOffsetParsed) time-string)
         ;=> #object[org.joda.time.DateTime 0x2de2f4db "2017-11-05T04:20:00.000-05:00"]

         (.parseDateTime (-> pattern DateTimeFormat/forPattern .withOffsetParsed) "2017-11-05T04:20+02")

         ; ISODateTimeFormat.dateTimeParser()

         )

;DateTimeZone forOffsetHoursMinutes(int hoursOffset, int minutesOffset)
(DateTimeZone/forOffsetHoursMinutes -2 30)

;Parser must distinguish between relation bounded interval t1/t2, and timezone-id continent/city.
;Just looking for "/" is not sufficient any more.
;The following doesn't work because the current parser things / means interval
;(from-iso "2017-11-11T11:11-05:00[America/Denver]")

;Zone: 'Z' outputs offset without a colon, 'ZZ' outputs the offset with a colon, 'ZZZ' or more outputs the zone id.

