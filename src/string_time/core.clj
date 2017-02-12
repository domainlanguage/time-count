(ns string-time.core
  (:require [clj-time.format :refer [parse formatters]]
            [clj-time.core :as clj :refer [plus days interval start end]]))

(defn date?
  [t]
  (try
    (parse (formatters :date) t)
    (catch Exception e
      false))
  )

(defn canonical-date-interval
  [date-string]
  (let [i-start (parse (formatters :date) date-string)]
    (interval
      i-start
      (plus i-start (days 1)))))


(defn before?
  "Works only for dates right now"
  [t1 t2]
  ;precondition both must be valid time strings
  (let [i1 (canonical-date-interval t1)
        i2 (canonical-date-interval t2)]
    (and (clj/before? (start i1) (start i2))
         (not (clj/before? (start i2) (end i1))))))
