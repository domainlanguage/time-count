(ns time-count.core-tests
    (:require [midje.sweet :refer :all]
              [time-count.core :refer :all]
              [time-count.metajoda])
    (:import [time_count.metajoda MetaJodaTime]
             [org.joda.time DateTime]))

;;Note: MetaJodaTime is used because we must have a concrete instance.
;; There is nothing specific here to the metajoda implementation.

(fact "A relation-bounded interval defines a sequence of CountedTimes"
      (t-sequence {:starts (MetaJodaTime. (DateTime. 2016 12 31 0 0 0 0) [:day :month :year])
           :finishes (MetaJodaTime. (DateTime. 2017 1 2 0 0 0 0) [:day :month :year])})
      => [(MetaJodaTime. (DateTime. 2016 12 31 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (DateTime. 2017 1 1 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (DateTime. 2017 1 2 0 0 0 0) [:day :month :year])]

      (t-rev-sequence {:starts (MetaJodaTime. (DateTime. 2016 12 31 0 0 0 0) [:day :month :year])
                   :finishes (MetaJodaTime. (DateTime. 2017 1 2 0 0 0 0) [:day :month :year])})
      => [(MetaJodaTime. (DateTime. 2017 1 2 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (DateTime. 2017 1 1 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (DateTime. 2016 12 31 0 0 0 0) [:day :month :year])])
