(ns time-count.relation-bounded-intervals
  (:require [time-count.core :refer [relation SequenceTime map->RelationBoundedInterval]]))



(defn flatten-starts [interval]
  (if (or (satisfies? SequenceTime interval)
          (nil? (:starts interval))
          (satisfies? SequenceTime (:starts interval)))
    interval
    (recur (assoc interval :starts (-> interval :starts :starts)))))

(defn flatten-finishes [interval]
  (if (or (satisfies? SequenceTime interval)
          (nil? (:finishes interval))
          (satisfies? SequenceTime (:finishes interval)))
    interval
    (recur (assoc interval :finishes (-> interval :finishes :finishes)))))


  (defn flatten-bounds
    [interval]
    (-> interval
        flatten-starts
        flatten-finishes))


;; BELOW HERE, USES NEW
(defn consistent?
  "This implementation works only for one case,
  where the :starts and :finishes are used.

  In Allen's algebra, we can compose relations:
  a(r)x, x(s)b => a(r.s)b
  Using one or both of {:starts a :finishes b} defines an interval x,
  which is the same as saying: a(starts)x, x(ended-by)b
  Now switching to notation from https://www.ics.uci.edu/~alspaugh/cls/shr/allen.html
  a(s)x, x(E)b and using the composition table in that article,
  (s).(E) => (pmo)
  So, the two relations that define x are consistent if a(pmo)b
  Switching back to the words used in this module, where p = :before, m = :meets, o = :overlaps,
  The relation of a and b must be in the set #{:before :meets :overlaps}.

  If there are fewer than two specified relations, no conflict is possible."

  [{:keys [starts finishes] :as interval-bounds}]
  (or (or (nil? starts) (nil? finishes))
      (#{:before :meets :overlaps} (relation starts finishes))))