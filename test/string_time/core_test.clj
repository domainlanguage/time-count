(ns string-time.core-test
  (:require [midje.sweet :refer :all]
            [string-time.core :refer :all]

            [clj-time.format :refer [parse]]))


(fact
  (date? "2016-08-18") => truthy
  (date? "2016-08-13T05:13:27.023Z") => falsey)

(fact "Dates shouldn't need a timezone to compare."
  (before? "2016-08-18" "2016-08-19") => truthy
  (before? "2016-08-19" "2016-08-18") => falsey)

