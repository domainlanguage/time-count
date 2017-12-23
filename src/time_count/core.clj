(ns time-count.core
  (:require [time-count.allens-algebra :refer [relation Interval]]))

;Clojure "protocol" isn't really a full protocol, it is just
; the interface for manipulating one data structure.
; Time would be an ok name for the whole protocol, but this
; particular, part is an element in a sequence of...
;  named times
;  countable times
;  time steps
;  sequential intervals
;  ticks
;  steps
;  enumerated time
;  time member (of sequence)
;  nth time
;  nth interval
;  counted intervals
;  step intervals
;  ...
;
; In contrast to "relation-bounded intervals"
;


(defprotocol CountableTime
  (next-t [t])
  (prev-t [t])
  (nest [t scale] "Relation-bounded interval equal to the given
                     Time interval, but nesting the given scales.")
  (enclosing-immediate [t] "Immediate enclosing-immediate interval.")
  (place-values [t] "Pairs of [scale value] in order of nesting.")
  (to-nesting [t scales] "Map this time to a time in a sequence with another nesting."))

(defn t-sequence [{:keys [starts finishes]}]
  ; :pre must contain starts. starts/finishes must have same nesting.
  (cond
    (and starts finishes) (take-while #(#{:before :meets :equal} (relation % finishes)) (iterate next-t starts))
    starts (iterate next-t starts)
    :else nil))

(defn t-rev-sequence [{:keys [starts finishes]}]
  ; :pre must contain finishes. starts/finishes must have same nesting.
  (cond
    (and finishes starts) (take-while #(#{:after :met-by :equal} (relation % starts)) (iterate prev-t finishes))
    finishes (iterate prev-t finishes)
    :else nil))

(defn enclosing
  ([{:keys [dt nesting] :as t} scale]
   (cond
     (-> nesting first (= scale)) t
     (-> nesting count (< 2)) nil
     :else (recur (enclosing-immediate t) scale))))

(defn place-value
  [t scale]
  (-> t
      place-values
      (#(into {} %))
      scale))

(extend-type nil
  CountableTime
  (next-t [t] nil)
  (prev-t [t] nil)
  (nest [t scale] nil)
  (enclosing-immediate [t] nil)
  (place-values [t] [])
  (to-nesting [t scales] nil))
