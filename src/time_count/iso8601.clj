(ns time-count.iso8601
  (:require [time-count.core])
  (:import [time_count.core SequenceTime]))

(defprotocol ISO8601Mappable
  (to-iso [SequenceTime] "ISO 8601 string representation of the SequenceTime interval."))