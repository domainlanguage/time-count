(ns time-count.timezone-tests
  (:require [midje.sweet :refer :all]
            [time-count.core :refer [next-interval]]
            [time-count.iso8601 :refer :all]
            [time-count.metajoda]))

;; Key test will be to produce these sequences:
; ["2017-11-05T01:59-04:00[America/New_York]"
;  "2017-11-05T01:00-05:00[America/New_York]"
;  "2017-11-05T01:01-05:00[America/New_York]"]

; ["2017-11-05T01:59-05:00[America/New_York]"
;  "2017-11-05T02:00-05:00[America/New_York]"
;  "2017-11-05T02:01-05:00[America/New_York]"]


(fact "unqualified time or chronology time"
      (t-> "2017-11-05T01:59" next-interval) => "2017-11-05T02:00")

(fact "There is a sequence for a UTC offset made up of all times with that same offset."
      (t-> "2017-11-05T01:59-04:00" next-interval) => "2017-11-05T02:00-04:00")