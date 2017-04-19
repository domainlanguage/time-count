(ns time-count.allens-interval-algebra
  (:import [org.joda.time DateTime])
  (:require [time-count.time-count :refer [next-interval]])
  (:require [time-count.meta-joda :refer [destringify]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;  Allen's Interval Algebra  ;;;
;;;  Basic relations           ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn allens-mj [[^DateTime dt-a & nesting-a :as a] [^DateTime dt-b & nesting-b :as b]]
  (let [a-left dt-a
        [a-right] (next-interval a)
        b-left dt-b
        [b-right] (next-interval b)]
    (cond
      (and (.isEqual a-left b-left)
           (.isEqual a-right b-right)) :equal
      (.isBefore a-right b-left) :before
      (.isAfter a-left b-right) :after
      (.isEqual a-right b-left) :meets
      (.isEqual b-right a-left) :met-by
      (and (.isEqual a-left b-left)
           (.isBefore a-right b-right)) :starts
      (and (.isEqual a-left b-left)
           (.isAfter a-right b-right)) :started-by
      (and (.isEqual a-right b-right)
           (.isAfter a-left b-left)) :finishes
      (and (.isEqual a-right b-right)
           (.isBefore a-left b-left)) :finished-by
      (and (.isAfter a-left b-left)
           (.isBefore a-right b-right)) :during
      (and (.isBefore a-left b-left)
           (.isAfter a-right b-right)) :contains
      (and (.isBefore a-left b-left)
           (.isBefore a-right b-right)
           (.isAfter a-right b-left)) :overlaps
      (and (.isBefore b-left a-left)
           (.isBefore b-right a-right)
           (.isAfter b-right a-left)) :overlapped-by



      :else :TBD)
    ))

(defn relation [a b]
  (allens-mj
    (destringify a)
    (destringify b)))
