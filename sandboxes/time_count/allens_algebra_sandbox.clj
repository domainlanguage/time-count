(ns time-count.allens-algebra-sandbox
  (:require
    [time-count.allens-algebra :refer [relation]]
    [time-count.relation-bounded-intervals :refer :all]
    [time-count.metajoda :refer [->MetaJodaTime]]
    [time-count.core :refer [CountableTime]]
    [time-count.iso8601 :refer :all]))


;(t-> "2017")
;(relation (from-iso "2017") (from-iso "2018"))

(def lb (->RelationBound :starts (from-iso "2015")))

;(relate-to-bound lr (from-iso "2016"))

(def rb (->RelationBound :finishes (from-iso "2018")))

(def rbi (->RelationBoundedInterval2 lb rb))

;(relation-rbi-ct rbi (from-iso "2016"))

; The following should be equivalent (?)
(def rbi2 (->RelationBoundedInterval2
            (->RelationBound :meets (from-iso "2014"))
            (->RelationBound :finishes (from-iso "2018"))))

;(relation-rbi-ct rbi2 (from-iso "2016"))
