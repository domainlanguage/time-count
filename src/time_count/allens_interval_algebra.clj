(ns time-count.allens-interval-algebra
  (:import [org.joda.time DateTime])
  (:require [time-count.time-count :refer [next-interval]]
            [time-count.meta-joda :refer [mj-time?]]
            [time-count.iso-8601 :refer [iso-to-mj]])) ; TODO This iso dependency seems iffy


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;  Allen's Interval Algebra  ;;;
;;;  Basic relations           ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn relation-mj [[^DateTime dt-a & nesting-a :as a] [^DateTime dt-b & nesting-b :as b]]
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

(defn relation-str [a b]
  (relation-mj
    (iso-to-mj a)
    (iso-to-mj b)))

(defn starts-to-dt-left [an-interval]
  (if (mj-time? an-interval)
    (first an-interval)
    (recur (:starts an-interval))))

(defn ends-to-dt-right [an-interval]
  (if (mj-time? an-interval)
    (-> an-interval next-interval first)
    (recur (:finishes an-interval))))

(defn to-dt-left-right [an-interval]
  {:dt-left (starts-to-dt-left an-interval)
   :dt-right (ends-to-dt-right an-interval)})


(defn- relation-dts [a-left a-right b-left b-right]
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
    )

(defn relation-gen [a b]
              (let [{a-left :dt-left a-right :dt-right} (to-dt-left-right a)
                    {b-left :dt-left b-right :dt-right} (to-dt-left-right b)]
                (relation-dts a-left a-right b-left b-right)))

(defn relation-gen-str [a b]
  (relation-gen
    (iso-to-mj a)
    (iso-to-mj b)))


(defn consistent-starts-finishes?
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
  (or (-> interval-bounds keys count (< 2))
      (#{:before :meets :overlaps} (relation-mj starts finishes))))
