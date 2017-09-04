(ns time-count.core-tests
  (:require [midje.sweet :refer :all]
            [time-count.core :refer :all]
            [time-count.metajoda :refer [to-MetaJodaTime]])
  (:import [time_count.metajoda MetaJodaTime]
           [org.joda.time DateTime]))

;;Note: MetaJodaTime is used because we must have a concrete instance.
;; There is nothing specific here to the metajoda implementation.

(fact "A relation-bounded interval defines a sequence of CountedTimes"
      (t-sequence {:starts   (MetaJodaTime. (DateTime. 2016 12 31 0 0 0 0) [:day :month :year])
                   :finishes (MetaJodaTime. (DateTime. 2017 1 2 0 0 0 0) [:day :month :year])})
      => [(MetaJodaTime. (DateTime. 2016 12 31 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (DateTime. 2017 1 1 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (DateTime. 2017 1 2 0 0 0 0) [:day :month :year])]

      (t-rev-sequence {:starts   (MetaJodaTime. (DateTime. 2016 12 31 0 0 0 0) [:day :month :year])
                       :finishes (MetaJodaTime. (DateTime. 2017 1 2 0 0 0 0) [:day :month :year])})
      => [(MetaJodaTime. (DateTime. 2017 1 2 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (DateTime. 2017 1 1 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (DateTime. 2016 12 31 0 0 0 0) [:day :month :year])]

      (t-sequence (->RelationBoundedInterval
                    (MetaJodaTime. (DateTime. 2016 12 31 0 0 0 0) [:day :month :year])
                    (MetaJodaTime. (DateTime. 2017 1 2 0 0 0 0) [:day :month :year])))
      => [(MetaJodaTime. (DateTime. 2016 12 31 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (DateTime. 2017 1 1 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (DateTime. 2017 1 2 0 0 0 0) [:day :month :year])])

(fact "A SequenceTime nesting can be unwrapped"
      (enclosing-immediate (MetaJodaTime. (DateTime. 2017 8 20 0 0 0 0) [:day :month :year]))
      => (MetaJodaTime. (DateTime. 2017 8 1 0 0 0 0) [:month :year])
      (enclosing
        (MetaJodaTime. (DateTime. 2017 8 20 0 0 0 0) [:day :month :year])
        :year)
      => (MetaJodaTime. (DateTime. 2017 1 1 0 0 0 0) [:year]))

(facts "about place values"
       (fact "The time value could be represented as a map of place-values"
             (place-values (MetaJodaTime. (DateTime. 2017 2 28 0 0 0 0) [:day :month :year]))
             => [[:day 28] [:month 2] [:year 2017]]))

(facts "about mapping between nestings"
       (fact "In JodaTime, you have to look at place values to see the change."
             (-> (MetaJodaTime. (DateTime. 2017 4 25 0 0 0 0) [:day :month :year])
                 (to-nesting [:day :week :week-year]))
             => (MetaJodaTime. (DateTime. 2017 4 25 0 0 0 0) [:day :week :week-year])

             (-> (MetaJodaTime. (DateTime. 2017 4 25 0 0 0 0) [:day :month :year])
                 (to-nesting [:day :week :week-year])
                 place-values)
             => [[:day 2] [:week 17] [:week-year 2017]])

       (fact "Some nestings are mappable; most are not."
             (-> [[:day 25] [:month 4] [:year 2017]]
                 to-MetaJodaTime
                 (to-nesting [:day :year])
                 place-values)
             => [[:day 115] [:year 2017]]

             (-> [[:day 25] [:month 4] [:year 2017]]
                 to-MetaJodaTime
                 (to-nesting [:month :year]))
             => :no-mapping)


       (fact "Mapping is bidirectional"
             (-> [[:day 25] [:month 4] [:year 2017]]
                 to-MetaJodaTime
                 (to-nesting [:day :week :week-year])
                 place-values)
             => [[:day 2] [:week 17] [:week-year 2017]]

             (-> [[:day 2] [:week 17] [:week-year 2017]]
                 to-MetaJodaTime
                 (to-nesting [:day :month :year])
                 place-values)
             => [[:day 25] [:month 4] [:year 2017]])

       (fact "Even the top level of nesting must be mapped."
             (-> [[:day 1] [:month 1] [:year 2017]]
                 to-MetaJodaTime
                 (to-nesting [:day :week :week-year])
                 place-values)
             => [[:day 7] [:week 52] [:week-year 2016]]))