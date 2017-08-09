(ns time-count.stringifying-tests
  (:require [time-count.iso-8601 :refer :all]
            [midje.sweet :refer :all])
  (:import [org.joda.time DateTime]))


(fact "In cannonical string, least significant place is scale."
      (mj-to-iso [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])
      => "2017-01-10"
      (mj-to-iso [(DateTime. 2017 1 10 0 0 0 0) :month :year])
      => "2017-01")

(fact "Pattern can be recognized from string."
      (time-string-pattern "2017-02-13") => "yyyy-MM-dd"
      (time-string-pattern "2017-02") => "yyyy-MM"
      (time-string-pattern "2017-02-13T18:09") => "yyyy-MM-dd'T'HH:mm"
      (time-string-pattern "2017-W05-2") => "xxxx-'W'ww-e")

(fact "Parsing can be constrained to a specific pattern or left open."
      ((partial iso-to-mj "yyyy-MM") "2017-01")
      => [(DateTime. 2017 1 1 0 0 0 0) :month :year]
      ((partial iso-to-mj "yyyy-MM-dd") "2017-01-10")
      => [(DateTime. 2017 1 10 0 0 0 0) :day :month :year]
      (iso-to-mj "2017-01")
      => [(DateTime. 2017 1 1 0 0 0 0) :month :year]
      (iso-to-mj "2017-01-10")
      => [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])

(fact "Relation bounded intervals can be represented as ISO"
      (iso-to-relation-bounded-interval "2017-05-15/2017-05-17")
      => {:starts   [(DateTime. 2017 5 15 0 0 0 0) :day :month :year]
          :finishes [(DateTime. 2017 5 17 0 0 0 0) :day :month :year]}

      (relation-bounded-interval-to-iso {:starts   [(DateTime. 2017 5 15 0 0 0 0) :day :month :year]
                                         :finishes [(DateTime. 2017 5 17 0 0 0 0) :day :month :year]})
      => "2017-05-15/2017-05-17")

(fact "Parsing can infer common date-time or interval formats."
      (from-iso "2017-05-15")
      => [(DateTime. 2017 5 15 0 0 0 0) :day :month :year]
      (from-iso "2017-05-15/2017-05-17")
      => {:starts   [(DateTime. 2017 5 15 0 0 0 0) :day :month :year]
          :finishes [(DateTime. 2017 5 17 0 0 0 0) :day :month :year]})

(future-fact "Sequences can be converted both ways"
             (to-iso [[(DateTime. 2017 5 15 0 0 0 0) :day :month :year]
                      [(DateTime. 2017 5 17 0 0 0 0) :day :month :year]])
             => ["2017-05-15" "2015-05-17"]

             (from-iso ["2017-05-15" "2015-05-17"])
             => [[(DateTime. 2017 5 15 0 0 0 0) :day :month :year]
                 [(DateTime. 2017 5 17 0 0 0 0) :day :month :year]])





;;TODO Other valid ISO formats, such as abbreviated second date, or using period


