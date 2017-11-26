(ns time-count.core-tests
  (:require [midje.sweet :refer :all]
            [time-count.core :refer :all]
            [time-count.metajoda :refer [from-place-values]])
  (:import [time_count.metajoda MetaJodaTime]
           [org.joda.time LocalDateTime]))

;;Note: MetaJodaTime is used because we must have a concrete instance.
;; There is nothing specific here to the metajoda implementation.

;TODO tests of next-t and prev-t (move some from demo?)
;TODO tests of nesting and enclosing (unwrapping?)
;TODO maybe - put all CountableTimes in these test all in form of place-values

(fact "A relation-bounded interval defines a sequence of CountableTimes"

      (t-sequence (->RelationBoundedInterval
                    (MetaJodaTime. (LocalDateTime. 2016 12 31 0 0 0 0) [:day :month :year])
                    (MetaJodaTime. (LocalDateTime. 2017 1 2 0 0 0 0) [:day :month :year])))
      => [(MetaJodaTime. (LocalDateTime. 2016 12 31 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (LocalDateTime. 2017 1 1 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (LocalDateTime. 2017 1 2 0 0 0 0) [:day :month :year])]

      (t-sequence {:starts   (MetaJodaTime. (LocalDateTime. 2016 12 31 0 0 0 0) [:day :month :year])
                   :finishes (MetaJodaTime. (LocalDateTime. 2017 1 2 0 0 0 0) [:day :month :year])})
      => [(MetaJodaTime. (LocalDateTime. 2016 12 31 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (LocalDateTime. 2017 1 1 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (LocalDateTime. 2017 1 2 0 0 0 0) [:day :month :year])]

      (t-rev-sequence {:starts   (MetaJodaTime. (LocalDateTime. 2016 12 31 0 0 0 0) [:day :month :year])
                       :finishes (MetaJodaTime. (LocalDateTime. 2017 1 2 0 0 0 0) [:day :month :year])})
      => [(MetaJodaTime. (LocalDateTime. 2017 1 2 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (LocalDateTime. 2017 1 1 0 0 0 0) [:day :month :year])
          (MetaJodaTime. (LocalDateTime. 2016 12 31 0 0 0 0) [:day :month :year])])


(fact "A CountableTime nesting can be unwrapped"
      (enclosing-immediate (MetaJodaTime. (LocalDateTime. 2017 8 20 0 0 0 0) [:day :month :year]))
      => (MetaJodaTime. (LocalDateTime. 2017 8 1 0 0 0 0) [:month :year])
      (enclosing
        (MetaJodaTime. (LocalDateTime. 2017 8 20 0 0 0 0) [:day :month :year])
        :year)
      => (MetaJodaTime. (LocalDateTime. 2017 1 1 0 0 0 0) [:year]))


(facts "about place values"
       (fact "The time value could be represented as a map of place-values"
             (place-values (MetaJodaTime. (LocalDateTime. 2017 2 28 0 0 0 0) [:day :month :year]))
             => [[:day 28] [:month 2] [:year 2017]]))

(facts "about mapping between nestings"
       (fact "In JodaTime, you have to look at place values to see the change."
             (-> (MetaJodaTime. (LocalDateTime. 2017 4 25 0 0 0 0) [:day :month :year])
                 (to-nesting [:day :week :week-year]))
             => (MetaJodaTime. (LocalDateTime. 2017 4 25 0 0 0 0) [:day :week :week-year])

             (-> (MetaJodaTime. (LocalDateTime. 2017 4 25 0 0 0 0) [:day :month :year])
                 (to-nesting [:day :week :week-year])
                 place-values)
             => [[:day 2] [:week 17] [:week-year 2017]])

       (fact "Some nestings are mappable; most are not."
             (-> [[:day 25] [:month 4] [:year 2017]]
                 from-place-values
                 (to-nesting [:day :year])
                 place-values)
             => [[:day 115] [:year 2017]]

             (-> [[:day 25] [:month 4] [:year 2017]]
                 from-place-values
                 (to-nesting [:month :year]))
             => :no-mapping)


       (fact "Mapping is bidirectional"
             (-> [[:day 25] [:month 4] [:year 2017]]
                 from-place-values
                 (to-nesting [:day :week :week-year])
                 place-values)
             => [[:day 2] [:week 17] [:week-year 2017]]

             (-> [[:day 2] [:week 17] [:week-year 2017]]
                 from-place-values
                 (to-nesting [:day :month :year])
                 place-values)
             => [[:day 25] [:month 4] [:year 2017]])

       (fact "Even the top level of nesting must be mapped."
             (-> [[:day 1] [:month 1] [:year 2017]]
                 from-place-values
                 (to-nesting [:day :week :week-year])
                 place-values)
             => [[:day 7] [:week 52] [:week-year 2016]]))