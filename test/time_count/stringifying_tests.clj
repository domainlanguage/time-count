(ns time-count.stringifying-tests
  (:require [midje.sweet :refer :all]
            [time-count.core :refer [->RelationBoundedInterval map->RelationBoundedInterval]]
            [time-count.iso8601 :refer :all]
            [time-count.metajoda :refer [->MetaJodaTime]])
  (:import [org.joda.time DateTime]))


(fact "In cannonical string, least significant place is scale."
      (to-iso (->MetaJodaTime (DateTime. 2017 1 10 0 0 0 0) [:day :month :year]))
      => "2017-01-10"
      (to-iso (->MetaJodaTime (DateTime. 2017 1 10 0 0 0 0) [:month :year]))
      => "2017-01")

(fact "Pattern can be recognized from string."
      (time-string-pattern "2017-02-13") => "yyyy-MM-dd"
      (time-string-pattern "2017-02") => "yyyy-MM"
      (time-string-pattern "2017-02-13T18:09") => "yyyy-MM-dd'T'HH:mm"
      (time-string-pattern "2017-W05-2") => "xxxx-'W'ww-e")

(fact "Parsing can be constrained to a specific pattern or left open."
      ((iso-parser "yyyy-MM") "2017-01")
      => (->MetaJodaTime (DateTime. 2017 1 1 0 0 0 0) [:month :year])
      ((iso-parser "yyyy-MM-dd") "2017-01-10")
      => (->MetaJodaTime (DateTime. 2017 1 10 0 0 0 0) [:day :month :year])
      (from-iso-sequence-time "2017-01")
      => (->MetaJodaTime (DateTime. 2017 1 1 0 0 0 0) [:month :year])
      (from-iso-sequence-time "2017-01-10")
      => (->MetaJodaTime (DateTime. 2017 1 10 0 0 0 0) [:day :month :year]))

(fact "Relation bounded intervals can be represented as ISO"
      (from-iso-to-relation-bounded-interval "2017-05-15/2017-05-17")
      => (->RelationBoundedInterval
           (->MetaJodaTime (DateTime. 2017 5 15 0 0 0 0) [:day :month :year])
           (->MetaJodaTime (DateTime. 2017 5 17 0 0 0 0) [:day :month :year]))

      (to-iso (->RelationBoundedInterval
                (->MetaJodaTime (DateTime. 2017 5 15 0 0 0 0) [:day :month :year])
                (->MetaJodaTime (DateTime. 2017 5 17 0 0 0 0) [:day :month :year])))
      => "2017-05-15/2017-05-17")

(fact "ISO 8601 doesn't seem to have a one-sided interval format. We use - to indicate a missing bound."
      (from-iso-to-relation-bounded-interval "2017-05-15/-")
      => (map->RelationBoundedInterval {:starts (->MetaJodaTime (DateTime. 2017 5 15 0 0 0 0) [:day :month :year])})
      (from-iso-to-relation-bounded-interval "-/2017-05-17")
      => (map->RelationBoundedInterval {:finishes (->MetaJodaTime (DateTime. 2017 5 17 0 0 0 0) [:day :month :year])})
      (to-iso (map->RelationBoundedInterval {:starts (->MetaJodaTime (DateTime. 2017 5 15 0 0 0 0) [:day :month :year])}))
      => "2017-05-15/-"
      (to-iso (map->RelationBoundedInterval {:finishes (->MetaJodaTime (DateTime. 2017 5 17 0 0 0 0) [:day :month :year])}))
      => "-/2017-05-17")

(fact "Parsing can infer common ISO 8601 date-time or interval formats."
      (from-iso "2017-05-15")
      => (->MetaJodaTime (DateTime. 2017 5 15 0 0 0 0) [:day :month :year])
      (from-iso "2017-05-15/2017-05-17")
      => {:starts   (->MetaJodaTime (DateTime. 2017 5 15 0 0 0 0) [:day :month :year])
          :finishes (->MetaJodaTime (DateTime. 2017 5 17 0 0 0 0) [:day :month :year])})


(future-fact "Sequences can be converted both ways"
             (to-iso [[(DateTime. 2017 5 15 0 0 0 0) :day :month :year]
                      [(DateTime. 2017 5 17 0 0 0 0) :day :month :year]])
             => ["2017-05-15" "2015-05-17"]

             (from-iso ["2017-05-15" "2015-05-17"])
             => [[(DateTime. 2017 5 15 0 0 0 0) :day :month :year]
                 [(DateTime. 2017 5 17 0 0 0 0) :day :month :year]])





;;TODO Other valid ISO formats, such as abbreviated second date, or using period


