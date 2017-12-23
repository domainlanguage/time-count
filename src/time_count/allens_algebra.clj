(ns time-count.allens-algebra
  (:require [time-count.allens-composition-table :refer :all]))

(defprotocol Interval
  (relation [t1 t2] "Return one of Allen's 13 basic relations."))

(def converse-relation
  {:equal         :equal
   :before        :after
   :after         :before
   :meets         :met-by
   :met-by        :meets
   :starts        :started-by
   :started-by    :starts
   :finishes      :finished-by
   :finished-by   :finishes
   :during        :contains
   :contains      :during
   :overlaps      :overlapped-by
   :overlapped-by :overlaps})

(defn composition [r1 r2]
  (set (map mathy-to-readable
            (composition-of-basic-relations-concise
              (readable-to-mathy r1) (readable-to-mathy r2)))))


