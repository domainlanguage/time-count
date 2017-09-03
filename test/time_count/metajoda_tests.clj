(ns time-count.metajoda-tests
  (:require [clojure.test :refer :all]
            [time-count.metajoda :refer [->MetaJodaTime]]
            [time-count.core :refer :all]
            [time-count.iso8601 :refer :all])
  (:import [org.joda.time DateTime]))

(deftest nesting
  (is (= (map->RelationBoundedInterval {:starts   (->MetaJodaTime (DateTime. 2017 3 1 0 0 0 0) [:day :month :year])
          :finishes (->MetaJodaTime (DateTime. 2017 3 31 0 0 0 0) [:day :month :year])})
         (nest (->MetaJodaTime (DateTime. 2017 3 1 0 0 0 0) [:month :year]) :day)))
  (is (= (->MetaJodaTime (DateTime. 2017 3 1 0 0 0 0) [:month :year])
         (enclosing-immediate (->MetaJodaTime (DateTime. 2017 3 3 0 0 0 0) [:day :month :year])))))

(deftest stepping
  (is (= (->MetaJodaTime (DateTime. 2017 1 11 0 0 0 0) [:day :month :year])
         (next-interval (->MetaJodaTime (DateTime. 2017 1 10 0 0 0 0) [:day :month :year]))))
  (is (= (->MetaJodaTime (DateTime. 2017 1 1 0 0 0 0) [:day :month :year])
         (next-interval (->MetaJodaTime (DateTime. 2016 12 31 0 0 0 0) [:day :month :year]))))
  (is (= (->MetaJodaTime (DateTime. 2017 1 10 0 0 0 0) [:day :month :year])
         (prev-interval (->MetaJodaTime (DateTime. 2017 1 11 0 0 0 0) [:day :month :year]))))
  (is (= (->MetaJodaTime (DateTime. 2016 12 31 0 0 0 0) [:day :month :year])
         (prev-interval (->MetaJodaTime (DateTime. 2017 1 1 0 0 0 0) [:day :month :year])))))

(deftest allens-relations
  (is (= :after
         (relation
           (->MetaJodaTime (DateTime. 2017 3 1 0 0 0 0) [:month :year])
           (->MetaJodaTime (DateTime. 2017 1 1 0 0 0 0) [:month :year])))))
(is (= :meets
       (relation
         (->MetaJodaTime (DateTime. 2017 2 1 0 0 0 0) [:month :year])
         (->MetaJodaTime (DateTime. 2017 3 1 0 0 0 0) [:month :year]))))


;; ISO 8601 representation
(is (= "2017-01-11"
       (to-iso (->MetaJodaTime (DateTime. 2017 1 11 0 0 0 0) [:day :month :year]))))
(is (= (->MetaJodaTime (DateTime. 2017 1 11 0 0 0 0) [:day :month :year])
       (from-iso-sequence-time "2017-01-11")))



(run-tests)
