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
          (not (contains? interval :ends))
          (mj-time? (interval :ends)))
    interval
    (recur (assoc interval :ends (-> interval :ends :ends)))))


(defn flatten-bounds
  [interval]
  (-> interval
      flatten-starts
      flatten-ends))
