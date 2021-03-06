(ns time-count.metajoda-tests
  (:require [time-count.allens-algebra :refer [relation]]
            [time-count.relation-bounded-intervals :refer [map->RelationBoundedInterval]]
            [time-count.metajoda :refer :all]               ;[->MetaJodaTime]]
            [time-count.core :refer :all]
            [time-count.iso8601 :refer :all]
            [midje.sweet :refer :all]
            [clojure.test :refer :all])
  (:import [org.joda.time LocalDateTime]))

(deftest nesting
  (is (= (map->RelationBoundedInterval {:starts   (->MetaJodaTime (LocalDateTime. 2017 3 1 0 0 0 0) [:day :month :year])
                                        :finishes (->MetaJodaTime (LocalDateTime. 2017 3 31 0 0 0 0) [:day :month :year])})
         (nest (->MetaJodaTime (LocalDateTime. 2017 3 1 0 0 0 0) [:month :year]) :day)))
  (is (= (->MetaJodaTime (LocalDateTime. 2017 3 1 0 0 0 0) [:month :year])
         (enclosing-immediate (->MetaJodaTime (LocalDateTime. 2017 3 3 0 0 0 0) [:day :month :year])))))

(deftest stepping
  (is (= (->MetaJodaTime (LocalDateTime. 2017 1 11 0 0 0 0) [:day :month :year])
         (next-t (->MetaJodaTime (LocalDateTime. 2017 1 10 0 0 0 0) [:day :month :year]))))
  (is (= (->MetaJodaTime (LocalDateTime. 2017 1 1 0 0 0 0) [:day :month :year])
         (next-t (->MetaJodaTime (LocalDateTime. 2016 12 31 0 0 0 0) [:day :month :year]))))
  (is (= (->MetaJodaTime (LocalDateTime. 2017 1 10 0 0 0 0) [:day :month :year])
         (prev-t (->MetaJodaTime (LocalDateTime. 2017 1 11 0 0 0 0) [:day :month :year]))))
  (is (= (->MetaJodaTime (LocalDateTime. 2016 12 31 0 0 0 0) [:day :month :year])
         (prev-t (->MetaJodaTime (LocalDateTime. 2017 1 1 0 0 0 0) [:day :month :year])))))

(deftest allens-relations
  (is (= :after
         (relation
           (->MetaJodaTime (LocalDateTime. 2017 3 1 0 0 0 0) [:month :year])
           (->MetaJodaTime (LocalDateTime. 2017 1 1 0 0 0 0) [:month :year])))))
(is (= :meets
       (relation
         (->MetaJodaTime (LocalDateTime. 2017 2 1 0 0 0 0) [:month :year])
         (->MetaJodaTime (LocalDateTime. 2017 3 1 0 0 0 0) [:month :year]))))


;; ISO 8601 representation
(is (= "2017-01-11"
       (to-iso (->MetaJodaTime (LocalDateTime. 2017 1 11 0 0 0 0) [:day :month :year]))))
(is (= (->MetaJodaTime (LocalDateTime. 2017 1 11 0 0 0 0) [:day :month :year])
       (from-iso-countable-time "2017-01-11")))


(run-tests)
