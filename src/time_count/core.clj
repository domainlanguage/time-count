(ns time-count.core
  (:import [org.joda.time DateTime]))

(defprotocol Time
  (next-interval [t])
  (prev-interval [t])
  (nest [t scale] "Relation-bounded interval equal to the given
                     Time interval, but nesting the given scales.")
  (enclosing [t] "Immediate enclosing interval.")
  (relation [t1 t2] "Return one of Allen's 13 basic relations."))

(defn t-sequence [{:keys [starts finishes]}]
  ; :pre must contain starts. starts/finishes must have same nesting.
  (if finishes
    (take-while #(#{:before :meets :equal} (relation % finishes)) (iterate next-interval starts))
    (iterate next-interval starts)))

(defn t-rev-sequence [{:keys [starts finishes]}]
  ; :pre must contain finishes. starts/finishes must have same nesting.
  (if starts
    (take-while #(#{:after :met-by :equal} (relation % starts)) (iterate prev-interval finishes))
    (iterate next-interval finishes)))

; Sketching protocol ...

; (nest :month 2017) => {:starts 2017-01 :ends 2017-12}

; (interval-sequence {:starts 2017-01 :ends 2017-12}) =>
;    2017-01 2017-02 ...

; (nested-seq :month 2017) => 2017-01 2017-02 ...

; (nest :day {:starts 2017-01 :ends 2017-12})
; => {:starts 2017-01-01 :ends 2017-12-31}

; (nest [:day :month] 2017)
; => {:starts 2017-01-01 :finishes 2017-12-31}

