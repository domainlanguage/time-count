(ns time-count.stringifying-tests
  (:require [time-count.meta-joda :refer :all]
            [midje.sweet :refer :all])
  (:import [org.joda.time DateTime]))


(fact "In cannonical string, least significant place is scale."
      (stringify [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])
      => "2017-01-10"
      (stringify [(DateTime. 2017 1 10 0 0 0 0) :month :year])
      => "2017-01")

(fact "Pattern can be recognized from string."
      (time-string-pattern "2017-02-13") => "yyyy-MM-dd"
      (time-string-pattern "2017-02") => "yyyy-MM"
      (time-string-pattern "2017-02-13T18:09") => "yyyy-MM-dd'T'HH:mm"
      (time-string-pattern "2017-W05-2") => "xxxx-'W'ww-e")

(fact "Parsing can be constrained to a specific pattern or left open."
      ((partial destringify "yyyy-MM") "2017-01")
      => [(DateTime. 2017 1 1 0 0 0 0) :month :year]
      ((partial destringify "yyyy-MM-dd") "2017-01-10")
      => [(DateTime. 2017 1 10 0 0 0 0) :day :month :year]
      (destringify "2017-01")
      => [(DateTime. 2017 1 1 0 0 0 0) :month :year]
      (destringify "2017-01-10")
      => [(DateTime. 2017 1 10 0 0 0 0) :day :month :year])


(fact "intervals can be represented as ISO"
      (iso-interval-to-meta-joda "2017-05-15/2017-05-17")
      => {:start [(DateTime. 2017 5 15 0 0 0 0) :day :month :year]
          :end [(DateTime. 2017 5 17 0 0 0 0) :day :month :year]}

      (meta-joda-interval-to-iso {:start [(DateTime. 2017 5 15 0 0 0 0) :day :month :year]
                                  :end   [(DateTime. 2017 5 17 0 0 0 0) :day :month :year]})
      => "2017-05-15/2017-05-17")



;;TODO Other valid ISO formats, such as abbreviated second date, or using period


