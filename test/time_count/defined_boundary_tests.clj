(ns time-count.defined-boundary-tests
  (:require [time-count.meta-joda :refer [stringify destringify]]
            [time-count.allens-interval-algebra :refer [relation-bounded consistent-starts-ends?]]
            [midje.sweet :refer :all]))


; named interval, part of a sequence of "meeting" intervals
;  -next-interval
;  -interval-seq
;  -enclosing
;  -nested (scale)
;  -allen's relation
;
; bounded interval, defined in terms of two intervals,
;  lower bound either "meets" or "starts" interval.
;  upper bound either "ends" or "met-by" interval.
;  -TODO What are the compatibility rules for the two bounds? (e.g. same nesting, mappable nesting ...)
;  -TODO interval-seq can take either kind (given same nesting?)
;  -TODO allen's relations can take either kind, or mix (use more of the algebra?)

;; Perhaps boundary relations may not bound both sides.
;; {:starts 2017} could be 2017-infinity
;; {:ends 2017} could be beginning of time-2017
;; {} could be all of time

;; If we define an interval with :starts and :ends,
;; how do we know the two are consistent-starts-ends?
(fact "Boundary relations must be consistent"
      (consistent-starts-ends? {:starts (destringify "2016") :ends (destringify "2018")}) => truthy
      (consistent-starts-ends? {:starts (destringify "2019") :ends (destringify "2016")}) => falsey
      (consistent-starts-ends? {:starts (destringify "2016") :ends (destringify "2017-06")}) => truthy
      (consistent-starts-ends? {:starts (destringify "2016") :ends (destringify "2016-06")}) => falsey
      (consistent-starts-ends? {:starts (destringify "2017")}) => truthy
      (consistent-starts-ends? {:ends (destringify "2017")}) => truthy)


(future-fact "Relations between two bounded intervals can be inferred."
      (relation-bounded {:starts (destringify "2016")
                         :ends (destringify "2017")}
                        {:starts (destringify "2019")
                         :ends (destringify "2019")})
      => :before)

;;How broad should we try to go? :starts / :ends only?
(future-fact "Any pair of consistent, fully-defined intervals have a relation."
      (relation-bounded
        {:starts (destringify "2016")
         :ends (destringify "2017")}
        {:meets (destringify "2015")
         :met-by (destringify "2018")})=> :equal)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; NOTE: Pairs like :after/before would not
;; fully specify the interval
;; For example
;; {:after 2016 :before 2018} equals 2017?
;; No, 2016 meets 2017 meets 2018.
;; We don't want to allow ambiguous or overly confusing cases.

;; What about consistent, but redundant ones?
;; {:after 2014 :meets 2016 :met-by 2018} equals 2017
;; The :after is irrelevant. In principle we could allow these.

