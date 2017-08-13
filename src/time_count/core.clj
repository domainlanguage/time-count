(ns time-count.core
  (:import [org.joda.time DateTime]))

(defprotocol Time
  (next-interval [t])
  (prev-interval [t]))

; Sketching protocol ...

; (nest :month 2017) => {:starts 2017-01 :ends 2017-12}

; (interval-sequence {:starts 2017-01 :ends 2017-12}) =>
;    2017-01 2017-02 ...

; (nested-seq :month 2017) => 2017-01 2017-02 ...

; (nest :day {:starts 2017-01 :ends 2017-12})
; => {:starts 2017-01-01 :ends 2017-12-31}

; (nest [:day :month] 2017)
; => {:starts 2017-01-01 :finishes 2017-12-31}

