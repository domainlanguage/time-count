(ns time-count.core
  (:import [org.joda.time DateTime]))

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

(defprotocol Interval
  (relation [t1 t2] "Return one of Allen's 13 basic relations."))

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

; Sketching protocol ...

; (nest :month 2017) => {:starts 2017-01 :ends 2017-12}

; (interval-sequence {:starts 2017-01 :ends 2017-12}) =>
;    2017-01 2017-02 ...

; (nested-seq :month 2017) => 2017-01 2017-02 ...

; (nest :day {:starts 2017-01 :ends 2017-12})
; => {:starts 2017-01-01 :ends 2017-12-31}

; (nest [:day :month] 2017)
; => {:starts 2017-01-01 :finishes 2017-12-31}

(def inverse-relation
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

(defn relation-rbi-st
  [{a :starts b :finishes} y]
  :not-implemented-yet)


;TODO Put the following in a different ns. allens_algebra or relation-bounded-interval?
(extend-protocol Interval
  RelationBoundedInterval
  (relation [t1 t2]
    (cond
      (satisfies? CountableTime t2) (relation-rbi-st t1 t2)
      :else (relation-rbi-rbi t1 t2))))

