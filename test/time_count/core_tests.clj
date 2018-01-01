(ns time-count.core-tests
  (:require [midje.sweet :refer :all]
            [clojure.algo.generic.functor :refer [fmap]]
            [time-count.core :refer :all]
            [time-count.metajoda :refer [from-place-values]])
  (:import [time_count.metajoda MetaJodaTime]
           [org.joda.time LocalDateTime]))

;;Note: MetaJodaTime is used because we must have a concrete instance.
;; There is nothing specific here to the metajoda implementation.
;; TODO Maybe put all CountableTimes in these test all in form of place-values


(facts "about CountableTime as part of a sequence"
       (fact "We can progress through the sequence using only an element from it (a CountableTime)."
             (next-t (MetaJodaTime. (LocalDateTime. 2017 8 20 0 0 0 0) [:day :month :year]))
             => (MetaJodaTime. (LocalDateTime. 2017 8 21 0 0 0 0) [:day :month :year])
             (prev-t (MetaJodaTime. (LocalDateTime. 2017 8 21 0 0 0 0) [:day :month :year]))
             => (MetaJodaTime. (LocalDateTime. 2017 8 20 0 0 0 0) [:day :month :year]))

       (fact "We can operate on the sequence itself."
             (take 3 (t-sequence {:starts (MetaJodaTime. (LocalDateTime. 2016 12 31 0 0 0 0) [:day :month :year])}))
             => [(MetaJodaTime. (LocalDateTime. 2016 12 31 0 0 0 0) [:day :month :year])
                 (MetaJodaTime. (LocalDateTime. 2017 1 1 0 0 0 0) [:day :month :year])
                 (MetaJodaTime. (LocalDateTime. 2017 1 2 0 0 0 0) [:day :month :year])]

             (take 3 (t-rev-sequence {:finishes (MetaJodaTime. (LocalDateTime. 2017 1 2 0 0 0 0) [:day :month :year])}))
             => [(MetaJodaTime. (LocalDateTime. 2017 1 2 0 0 0 0) [:day :month :year])
                 (MetaJodaTime. (LocalDateTime. 2017 1 1 0 0 0 0) [:day :month :year])
                 (MetaJodaTime. (LocalDateTime. 2016 12 31 0 0 0 0) [:day :month :year])]

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
       )

(fact "A sequence of :day can be nested within either a :month or a :year"
      (nest
        (MetaJodaTime. (LocalDateTime. 2017 8 1 0 0 0 0) [:month :year])
        :day)
      => {:starts   (MetaJodaTime. (LocalDateTime. 2017 8 1 0 0 0 0) [:day :month :year])
          :finishes (MetaJodaTime. (LocalDateTime. 2017 8 31 0 0 0 0) [:day :month :year])}

      (nest
        (MetaJodaTime. (LocalDateTime. 2017 1 1 0 0 0 0) [:year])
        :day)
      => {:starts   (MetaJodaTime. (LocalDateTime. 2017 1 1 0 0 0 0) [:day :year])
          :finishes (MetaJodaTime. (LocalDateTime. 2017 12 31 0 0 0 0) [:day :year])}

      ;Representing time with place-values makes this operation much clearer!
      (-> [[:month 8] [:year 2017]]
          from-place-values
          (nest :day)
          (#(into {} %))
          (#(fmap place-values %)))                         ;TODO Maybe try to have a place-values function for RelationBoundedInterval
      => {:starts   [[:day 1] [:month 8] [:year 2017]]      ;But then I'd need to figure out an isomorphic representation.
          :finishes [[:day 31] [:month 8] [:year 2017]]}

      (-> [[:year 2017]]
          from-place-values
          (nest :day)
          (#(into {} %))
          (#(fmap place-values %)))
      => {:starts   [[:day 1] [:year 2017]]
          :finishes [[:day 365] [:year 2017]]})

(fact "Only nestings defined in the system will produce a result."
      (-> [[:year 2017]]
          from-place-values
          (nest :hour))
      => nil)                                               ;There is no nesting set up for [:hour :year] TODO Is there a return value better than nil?




(fact "A CountableTime nesting can be unwrapped"
      (enclosing-immediate (MetaJodaTime. (LocalDateTime. 2017 8 20 0 0 0 0) [:day :month :year]))
      => (MetaJodaTime. (LocalDateTime. 2017 8 1 0 0 0 0) [:month :year])
      (enclosing
        (MetaJodaTime. (LocalDateTime. 2017 8 20 0 0 0 0) [:day :month :year])
        :year)
      => (MetaJodaTime. (LocalDateTime. 2017 1 1 0 0 0 0) [:year])
      (enclosing
        (MetaJodaTime. (LocalDateTime. 2017 8 20 0 0 0 0) [:day :month :year])
        :day)
      => (MetaJodaTime. (LocalDateTime. 2017 8 20 0 0 0 0) [:day :month :year])
      (enclosing
        (MetaJodaTime. (LocalDateTime. 2017 8 20 0 0 0 0) [:day :month :year])
        :hour)
      => nil)


(facts "about place values"
       (fact "A countable time value could be represented as a vector of place-values"
             (place-values (MetaJodaTime. (LocalDateTime. 2017 2 28 0 0 0 0) [:day :month :year]))
             => [[:day 28] [:month 2] [:year 2017]]
             (from-place-values [[:day 28] [:month 2] [:year 2017]])
             => (MetaJodaTime. (LocalDateTime. 2017 2 28 0 0 0 0) [:day :month :year]))
       (fact
         (-> (from-place-values [[:day 28] [:month 2] [:year 2017]])
             (place-value :month))
         => 2
         (-> (from-place-values [[:day 28] [:month 2] [:year 2017]])
             (place-value :hour))
         => nil))

(facts "Unwrapping the nesting can be seen clearly in place values (TODO Consider implementing it using place-values)"
       (-> [[:day 28] [:month 2] [:year 2017]]
           from-place-values
           enclosing-immediate
           place-values)
       => [[:month 2] [:year 2017]]
       (-> [[:day 28] [:month 2] [:year 2017]]
           from-place-values
           (enclosing :year)
           place-values)
       => [[:year 2017]]
       (-> [[:day 28] [:month 2] [:year 2017]]
           from-place-values
           (enclosing :day)
           place-values)
       => [[:day 28] [:month 2] [:year 2017]]
       (-> [[:day 28] [:month 2] [:year 2017]]
           from-place-values
           (enclosing :hour)
           place-values)
       => [])


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