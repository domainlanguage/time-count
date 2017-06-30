(ns time-count.relation-bounded-intervals
  (:require [time-count.meta-joda :refer [mj-time?]]))

(defn flatten-starts [interval]
  (if (or (mj-time? interval)
          (not (contains? interval :starts))
          (mj-time? (interval :starts)))
    interval
    (recur (assoc interval :starts (-> interval :starts :starts)))))

(defn flatten-ends [interval]
  (if (or (mj-time? interval)
          (not (contains? interval :finishes))
          (mj-time? (interval :finishes)))
    interval
    (recur (assoc interval :finishes (-> interval :finishes :finishes)))))


(defn flatten-bounds
  [interval]
  (-> interval
      flatten-starts
      flatten-ends))
