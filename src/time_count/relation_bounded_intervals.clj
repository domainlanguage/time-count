(ns time-count.relation-bounded-intervals
  (:require [time-count.allens-algebra :refer [Interval relation converse-relation composition]]
            [time-count.allens-composition-table :refer [full]]
            [time-count.core :refer [CountableTime]]
            [clojure.set :refer [intersection]]))

;RelationBoundedInterval is used to express an interval that
;might not be fully known in terms of its relations to specific intervals.
;The RelationBoundedInterval is defined in terms of one or two RelationBounds,
; each of which is a time and the relationship of that time to the bounded interval.
;
; Take an example where we are describing interval X in terms of its relations
; with other intervals.The RelationBound {:starts 2017} means
; that 2017 starts X.
;
; The behavior can be understood intuitively, but here is some math
; behind the code in this namespace.
;
; More generally, then, if a bound referring to an interval x is {r b}
; (and using the notation used in https://www.ics.uci.edu/~alspaugh/cls/shr/allen.html)
; then {r b} is the same as b(r)x.
; The point is to be able to infer the relation, s,
; between x and some other interval, y.
; So, given b(r)x and x(s)y, what is s?
; b(r)x = x(!r)b
; x(!r)b, b(q)y => x(!r.q)y
;
; So for {:starts 2017}, r=starts, b=2017
; Then if we want the relationship to 2018,
; y=2018
; !r => S (aka started-by)
; b(q)y => q=m (aka meets)
; (!r.q) = (S.m) = (oFD) (see compositon table in link above.)
; x(oFD)y
; So {:starts 2017} either
; overlaps, is finished-by or contains 2018,
;
; Similarly,
; {:finishes 2020}, 2020 finishes x
; for y=2018 again
; x(F)b, b(P)y => x(F.P)y = x(DSOMP)y
;
; Combining these into a full RelationBoundedInterval,
; rbi = {:starts 2017 :finishes :2020},
; fully defines x.
; Again, looking for the relation of x(r)y, where y=2018,
; we take the intersection of the relations of the two
; individual bounds,
; (oFD)^(DSOMP) = (D) (aka :contains)
; 2017/2020 contains 2018


(defrecord RelationBound [relation t])

(defn relate-bound-to-ct [rb y]
  (let [conv-r (converse-relation (:relation rb))
        b (:t rb)
        s (relation b y)]
    (composition conv-r s)))

(defn relation-rbi-ct2 [{:keys [left-bound right-bound]} ct]
  (intersection
    (if left-bound (relate-bound-to-ct left-bound ct) full)
    (if right-bound (relate-bound-to-ct right-bound ct) full)))

;---Here's an experimental
(defrecord RelationBoundedInterval2 [left-bound right-bound])

(extend-protocol Interval
  RelationBoundedInterval2
  (relation [t1 t2]
    (cond
      (satisfies? CountableTime t2) (relation-rbi-ct2 t1 t2)
      :else :TBD)))
;---

(defn flatten-starts [interval]
  (if (or (satisfies? CountableTime interval)
          (nil? (:starts interval))
          (satisfies? CountableTime (:starts interval)))
    interval
    (recur (assoc interval :starts (-> interval :starts :starts)))))

(defn flatten-finishes [interval]
  (if (or (satisfies? CountableTime interval)
          (nil? (:finishes interval))
          (satisfies? CountableTime (:finishes interval)))
    interval
    (recur (assoc interval :finishes (-> interval :finishes :finishes)))))


(defn flatten-bounds
  [interval]
  (-> interval
      flatten-starts
      flatten-finishes))


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


(defrecord RelationBoundedInterval [starts finishes])

(defn relation-rbi-rbi
  [{a :starts b :finishes} {c :starts d :finishes}]
  (let [aec (= :equal (relation a c))
        bed (= :equal (relation b d))
        ]
    (cond
      (and aec bed) :equal
      (= :before (relation b c)) :before
      (= :after (relation a d)) :after
      (= :meets (relation b c)) :meets
      (= :met-by (relation a b)) :met-by
      (and aec (#{:before :meets} (relation b d))) :starts

      :else :error)))


(defn relation-rbi-ct
  [{a :starts b :finishes} y]
  (first (relation-rbi-ct2
           {:left-bound  (->RelationBound :starts a)
            :right-bound (->RelationBound :finishes b)}
           y)))


(extend-protocol Interval
  RelationBoundedInterval
  (relation [t1 t2]
    (cond
      (satisfies? CountableTime t2) (relation-rbi-ct t1 t2)
      :else (relation-rbi-rbi t1 t2))))

