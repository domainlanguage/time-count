(ns time-count.allens-interval-algebra
  (:import [org.joda.time DateTime])
  (:require [time-count.time-count :refer [next-interval]])
  (:require [time-count.meta-joda :refer [destringify]]))

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
    (destringify a)
    (destringify b)))

(defn relation-bounded
  "relate two intervals defined by lower and upper bounds,
  where keys are relations."
  [{:keys [starts-a ends-a meets-a met-by-a]}
   {:keys [starts-b ends-b meets-b met-by-b]}]
  :unimplemented
  ; TODO Study more of Allen's algebra
  )

(defn consistent-starts-ends?
  "This implementation works only for one case,
  where the :starts and :ends are used.

  In Allen's algebra, we can compose relations:
  a(r)x, x(s)b => a(r.s)b
  Using one or both of {:starts a :ends b} defines an interval x,
  which is the same as saying: a(starts)x, x(ended-by)b
  Now switching to notation from https://www.ics.uci.edu/~alspaugh/cls/shr/allen.html
  a(s)x, x(E)b and using the composition table in that article,
  (s).(E) => (pmo)
  So, the two relations that define x are consistent if a(pmo)b
  Switching back to the words used in this module, where p = :before, m = :meets, o = :overlaps,
  The relation of a and b must be in the set #{:before :meets :overlaps}.

  If there are fewer than two specified relations, no conflict is possible."

  [{:keys [starts ends] :as interval-bounds}]
  (or (-> interval-bounds keys count (< 2))
      (#{:before :meets :overlaps} (relation-mj starts ends))))
