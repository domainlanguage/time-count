(ns time-count.metajoda-tests
  (:require [clojure.test :refer :all]
            [time-count.metajoda]
            [time-count.core :refer :all])
  (:import [time_count.metajoda MetaJodaTime]
           [org.joda.time DateTime]))

(deftest addition
  (is (= 4 (+ 2 2)))
  (is (= 7 (+ 3 4))))

(deftest stepping
  (is (= (MetaJodaTime. (DateTime. 2017 1 11 0 0 0 0) [:day :month :year])
         (next-interval (MetaJodaTime. (DateTime. 2017 1 10 0 0 0 0) [:day :month :year]))))
  (is (= (MetaJodaTime. (DateTime. 2017 1 1 0 0 0 0) [:day :month :year])
         (next-interval (MetaJodaTime. (DateTime. 2016 12 31 0 0 0 0) [:day :month :year]))))
  (is (= (MetaJodaTime. (DateTime. 2017 1 10 0 0 0 0) [:day :month :year])
         (prev-interval (MetaJodaTime. (DateTime. 2017 1 11 0 0 0 0) [:day :month :year]))))
  (is (= (MetaJodaTime. (DateTime. 2016 12 31 0 0 0 0) [:day :month :year])
         (prev-interval (MetaJodaTime. (DateTime. 2017 1 1 0 0 0 0) [:day :month :year])))))

(run-tests)
