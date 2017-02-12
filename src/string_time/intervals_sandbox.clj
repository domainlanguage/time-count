(ns string-time.intervals-sandbox
  (:require [midje.sweet :refer :all]))

(defn next-interval [[[step & rest] scales]]
  [(cons (inc step) rest) scales]
    )

(fact "regular cycles increment simply"
      (next-interval [[16 5] [:minute :hour]])
      => [[17 5] [:minute :hour]]
      (next-interval [[59 5] [:minute :hour]])
      => [[0 6] [:minute :hour]]
      )

; (next-month-str3 "2017-01-29")
; => "2017-02"
; (end-of-next-month "2017-01-29")
; => "2017-02-28"