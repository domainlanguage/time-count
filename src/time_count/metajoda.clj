(ns time-count.metajoda
  (:require [time-count.core :refer [Time]]
            [time-count.meta-joda :refer [scale-to-Period]]
            [time-count.time-count :refer [nested-first nested-last]]
            [time-count.allens-interval-algebra :refer [relation-mj]])
  (:import [org.joda.time DateTime]))

;; TODO Replace meta-joda require with new stuff in core
;; TODO Replace time-count require with new stuff here.
;; TODO Replace allens-interval-algebra require with new stuff somewhere.
;; TODO Add namespaced keywords to core.


(defrecord MetaJodaTime [^DateTime dt nesting]
  Time

  (next-interval [t]
    (MetaJodaTime.
      (.plus dt (scale-to-Period (first nesting)))
      nesting))

  (prev-interval [t]
    (MetaJodaTime.
      (.minus dt (scale-to-Period (first nesting)))
      nesting))

  (nest [t scale]
    (let [mjt (cons dt nesting)
          [dtf & new-nesting] (nested-first scale mjt)
          [dtl & _] (nested-last scale mjt)]

      {:starts   (MetaJodaTime. dtf new-nesting)
       :finishes (MetaJodaTime. dtl new-nesting)}))

  (enclosing [t]
    (let [[dtf & _] (nested-first (first nesting) (cons dt (rest nesting)))]
      (MetaJodaTime. dtf (rest nesting))))

  (relation [t1 t2]
    (relation-mj
      (cons (:dt t1) (:nesting t1))
      (cons (:dt t2) (:nesting t2)))))


