(ns time-count.stringifying-tests
  (:require [midje.sweet :refer :all]
            [time-count.core]
            [time-count.relation-bounded-intervals :refer [->RelationBoundedInterval map->RelationBoundedInterval]]
            [time-count.iso8601 :refer :all]
            [time-count.metajoda :refer [->MetaJodaTime]])
  (:import [org.joda.time LocalDateTime DateTime DateTimeZone]))


(fact "In cannonical string, least significant place is scale."
      (to-iso (->MetaJodaTime (LocalDateTime. 2017 1 10 0 0 0 0) [:day :month :year]))
      => "2017-01-10"
      (to-iso (->MetaJodaTime (LocalDateTime. 2017 1 10 0 0 0 0) [:month :year]))
      => "2017-01")

(fact "Pattern can be recognized from string."
      (time-string-pattern "2017-02-13") => "yyyy-MM-dd"
      (time-string-pattern "2017-02") => "yyyy-MM"
      (time-string-pattern "2017-02-13T18:09") => "yyyy-MM-dd'T'HH:mm"
      (time-string-pattern "2017-W05-2") => "xxxx-'W'ww-e")

(fact "Parsing can be constrained to a specific pattern or left open."
      ((iso-parser "yyyy-MM") "2017-01")
      => (->MetaJodaTime (LocalDateTime. 2017 1 1 0 0 0 0) [:month :year])
      ((iso-parser "yyyy-MM-dd") "2017-01-10")
      => (->MetaJodaTime (LocalDateTime. 2017 1 10 0 0 0 0) [:day :month :year])
      (from-iso-countable-time "2017-01")
      => (->MetaJodaTime (LocalDateTime. 2017 1 1 0 0 0 0) [:month :year])
      (from-iso-countable-time "2017-01-10")
      => (->MetaJodaTime (LocalDateTime. 2017 1 10 0 0 0 0) [:day :month :year]))

(fact "Relation bounded intervals can be represented as ISO"
      (from-iso "2017-05-15/2017-05-17")
      => (->RelationBoundedInterval
           (->MetaJodaTime (LocalDateTime. 2017 5 15 0 0 0 0) [:day :month :year])
           (->MetaJodaTime (LocalDateTime. 2017 5 17 0 0 0 0) [:day :month :year]))

      (to-iso (->RelationBoundedInterval
                (->MetaJodaTime (LocalDateTime. 2017 5 15 0 0 0 0) [:day :month :year])
                (->MetaJodaTime (LocalDateTime. 2017 5 17 0 0 0 0) [:day :month :year])))
      => "2017-05-15/2017-05-17")

(fact "ISO 8601 doesn't seem to have a one-sided interval format. We use - to indicate a missing bound."
      (from-iso "2017-05-15/-")
      => (map->RelationBoundedInterval {:starts (->MetaJodaTime (LocalDateTime. 2017 5 15 0 0 0 0) [:day :month :year])})
      (from-iso "-/2017-05-17")
      => (map->RelationBoundedInterval {:finishes (->MetaJodaTime (LocalDateTime. 2017 5 17 0 0 0 0) [:day :month :year])})
      (to-iso (map->RelationBoundedInterval {:starts (->MetaJodaTime (LocalDateTime. 2017 5 15 0 0 0 0) [:day :month :year])}))
      => "2017-05-15/-"
      (to-iso (map->RelationBoundedInterval {:finishes (->MetaJodaTime (LocalDateTime. 2017 5 17 0 0 0 0) [:day :month :year])}))
      => "-/2017-05-17")

(fact "Parsing can infer common ISO 8601 date-time or interval formats."
      (from-iso "2017-05-15")
      => (->MetaJodaTime (LocalDateTime. 2017 5 15 0 0 0 0) [:day :month :year])
      (from-iso "2017-05-15/2017-05-17")
      => {:starts   (->MetaJodaTime (LocalDateTime. 2017 5 15 0 0 0 0) [:day :month :year])
          :finishes (->MetaJodaTime (LocalDateTime. 2017 5 17 0 0 0 0) [:day :month :year])})


(future-fact "Sequences can be converted both ways"
             (to-iso [[(LocalDateTime. 2017 5 15 0 0 0 0) :day :month :year]
                      [(LocalDateTime. 2017 5 17 0 0 0 0) :day :month :year]])
             => ["2017-05-15" "2015-05-17"]

             (from-iso ["2017-05-15" "2015-05-17"])
             => [[(LocalDateTime. 2017 5 15 0 0 0 0) :day :month :year]
                 [(LocalDateTime. 2017 5 17 0 0 0 0) :day :month :year]])


;;TODO Other valid ISO formats, such as abbreviated second date, or using period


(fact "A time string with time-zone information is parsed into three parts"
      (tz-split "2017-10-31T20:00") => ["2017-10-31T20:00"]
      (tz-split "2017-10-31T20:00-05") => ["2017-10-31T20:00"] ;Valid ISO 8601, but INVALID time-count
      (tz-split "2017-10-31T20:00-05:00") => ["2017-10-31T20:00" "-05:00"]
      ;;  (tz-split "2017-10-31T-05:00") => ["2017-10-31" "-05:00"]????  ;Invalid ISO 8601, but valid time-count???
      (tz-split "2017-10-31T20:00-05:00[America/New_York]") => ["2017-10-31T20:00" "-05:00" "America/New_York"]
      (tz-split "2017") => ["2017"]
      (tz-split "2017T-05:00") => ["2017" "-05:00"])

(fact "A string with no timezone info is represented as org.joda.time.LocalDateTime"
      (from-iso "2017-11-05T04:35")
      => (->MetaJodaTime (LocalDateTime. 2017 11 5 4 35 0 0) [:minute :hour :day :month :year])
      (to-iso (->MetaJodaTime (LocalDateTime. 2017 11 5 4 35 0 0) [:minute :hour :day :month :year]))
      => "2017-11-05T04:35")

(fact "A string with an offset is represented as DateTime with offset"
      (from-iso "2017-11-05T04:35-03:15")                   ;A weird mid-Atlantic offset for testing
      => (->MetaJodaTime (DateTime. 2017 11 5 4 35
                                    (DateTimeZone/forOffsetHoursMinutes -3 -15))
                         [:minute :hour :day :month :year])
      (to-iso (->MetaJodaTime (DateTime. 2017 11 5 4 35
                                         (DateTimeZone/forOffsetHoursMinutes -3 -15))
                              [:minute :hour :day :month :year]))
      => "2017-11-05T04:35-03:15")

(fact "The ends of a relation-bounded interval are separated by a slash."
      (split-relation-bounded-interval-iso "2017-10-31T20:00/2017-10-31T20:30")
      => ["2017-10-31T20:00" "2017-10-31T20:30"]
      (split-relation-bounded-interval-iso "2017-10-31T20:00")
      => ["2017-10-31T20:00"]
      (split-relation-bounded-interval-iso "2017-10-31T20:00-05:00[America/New_York]")
      => ["2017-10-31T20:00-05:00[America/New_York]"]
      (split-relation-bounded-interval-iso "2017-10-31T20:00-05:00[America/New_York]/2017-10-31T21:05-06:00[America/Chicago]")
      => ["2017-10-31T20:00-05:00[America/New_York]" "2017-10-31T21:05-06:00[America/Chicago]"]
      (split-relation-bounded-interval-iso "2017-10-31T20:00-05:00[America/New_York]/-")
      => ["2017-10-31T20:00-05:00[America/New_York]" "-"]
      (split-relation-bounded-interval-iso "-/2017-10-31T20:00-05:00[America/New_York]")
      => ["-" "2017-10-31T20:00-05:00[America/New_York]"])

(fact "Any iso representation should round-trip convert to and from other representations"
      (t-> "2017-11-15T20") => "2017-11-15T20"
      (t-> "2017-11-15T20:00") => "2017-11-15T20:00"
      (t-> "2017-11-15T20:00-05:00") => "2017-11-15T20:00-05:00"
      (t-> "2017-11-15T20:00-05:00[America/New_York]") => "2017-11-15T20:00-05:00[America/New_York]")

(fact "Maybe timezone is a suspect concept at date level or higher, but in this model, a day contains a nested sequence of hours, etc., so it has to be in a timezone."
      (t-> "2017T-05:00") => "2017T-05:00")