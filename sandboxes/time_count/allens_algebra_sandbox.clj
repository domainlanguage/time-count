(ns time-count.allens-algebra-sandbox
  (:require
    [time-count.allens-algebra :refer [relation]]
    [time-count.relation-bounded-intervals :refer :all]
    [time-count.metajoda :refer [->MetaJodaTime]]
    [time-count.core :refer [CountableTime]]
    [time-count.iso8601 :refer :all]))

;; NOTICE: This namespace is not really part of the project
;; It is not source, test, or even example
;; It's purpose it to make it convenient to do experiments.
;; (For example, lots of stuff is imported from other namespaces
;; for convenience, because minimizing dependencies doesn't matter here.)
;; As such, perhaps it should be removed from the repo.
;; For now, it remains here because the PURPOSE of time-count
;; is to explore and illustrate ideas.
;; So, don't expect this namespace to be tidy or even correct.
;; But maybe you'll want to fiddle around in it.

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
