(ns time-count.metajoda
  (:require [time-count.core :refer [Time]]
            [time-count.meta-joda :refer [scale-to-Period]])
  (:import [org.joda.time DateTime]))

;; TODO Replace meta-joda require with new stuff in core
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
      nesting)))
