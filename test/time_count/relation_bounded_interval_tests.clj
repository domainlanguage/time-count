(ns time-count.relation-bounded-interval-tests
  (:require [time-count.iso-8601-old :refer [from-iso]]
            [time-count.relation-bounded-intervals :refer [flatten-bounds]]
            [time-count.allens-interval-algebra :refer [relation-gen consistent-starts-finishes?]]
            [midje.sweet :refer :all]))


; scale-unit interval
; (part of a sequence of intervals that meet).
;  -next-interval
;  -interval-seq
;  -enclosing
;  -nested (scale)
;  -allen's relation

; relation-spec
;   tuple of relation and an interval
;   e.g. [:starts 2017], meaning that, for some interval x, "2017 starts x"
;        [:overlaps 2017] means that, for some interval x, "2017 overlaps x"
;   - satisfies
;     e.g. (satisfies [:starts 2017] 2017-01) => true
;          (satisfies [:overlaps 2017] 2017-01) => false
;   - combine with AND


; relation-bounded interval
;   defined by one or more relation-specs
;   to make it tractable, only :starts and :finishes
;   (might add :meets :met-by, a few others, later)
;   e.g. {:starts 2017}
;   - fully-specified (does it define a single interval?)
;   e.g. {:starts 2017 :finishes 2018}
;
;   To make it less confusing, both bounds must be same scale. That will be the scale of the relation-bounded interval.
;   - (scale {:starts 2017 :finishes 2018}) => :year
;  -TODO What are the compatibility rules for the two bounds? (e.g. same nesting, mappable nesting ...)
;   -interval-seq (requires :starts, :finishes is optional,
;         although interval-last could still have a value with :finishes and not :starts)
;   -nested (scale)
;   e.g. ((nested :month) {:starts 2017 :finishes 2018}) => {:starts 2017-01 :finishes 2018-12}
;   -enclosing (???)
;   e.g. (enclosing {:starts 2017-03 :finishes 2018-03}) => {:starts 2017 :finishes 2018} ???
;

;; Perhaps boundary relations may not bound both sides.
;; It is often the case we record a start time without knowing the end time.
;; This is a partial specification of an interval.
;; So, how to interpret the half-bounded intervals?
;; {:starts 2017} could be 2017-infinity
;; {:finishes 2017} could be beginning of time-2017
;; {} could be all of time
;; BUT I don't like that. Business domains don't really have "forever",
;; and certainly not beginning of time. What they do have is,
;;  an indefinite and unknown extent.
;; So, {:starts 2017} means 2017 was the start, and I don't know when the end is.
;  That is a common case, and sometimes awkward to express.
;  Likewise {:finishes 2017} means 2017 was the end and we don't know when it started.
;  Not knowing when it started does not imply that it might be infinitely old.
;

;; If we define an interval with :starts and :finishes,
;; how do we know the two are consistent-starts-ends?
(fact "Boundary relations must be consistent"
      (consistent-starts-finishes? {:starts (from-iso "2016") :finishes (from-iso "2018")}) => truthy
      (consistent-starts-finishes? {:starts (from-iso "2019") :finishes (from-iso "2016")}) => falsey
      (consistent-starts-finishes? {:starts (from-iso "2016") :finishes (from-iso "2017-06")}) => truthy
      (consistent-starts-finishes? {:starts (from-iso "2016") :finishes (from-iso "2016-06")}) => falsey
      (consistent-starts-finishes? {:starts (from-iso "2017")}) => truthy
      (consistent-starts-finishes? {:finishes (from-iso "2017")}) => truthy)

(fact "With boundaries restricted to starts/ends, deep compositions can be flattened"
      ;TODO How to verify consistency of compositions?
      (flatten-bounds {:starts
                       {:starts
                        {:starts (from-iso "2017-06") :finishes
                                 {:starts (from-iso "2017-07") :finishes (from-iso "2017-08")}}}
                       :finishes
                       {:starts (from-iso "2017-07") :finishes (from-iso "2017-10")}})
      => {:starts (from-iso "2017-06") :finishes (from-iso "2017-10")})

(fact "Relations between two bounded intervals can be inferred."
             (relation-gen {:starts (from-iso "2016")
                                :finishes   (from-iso "2018")}
                               {:starts (from-iso "2017")
                                :finishes   (from-iso "2019")})
             => :overlaps)

;(fact "Relations between two bounded intervals can be inferred."
;      (relation-gen (from-iso "2016/2018")
;                    {:starts (from-iso "2017")
;                     :finishes   (from-iso "2019")})
;      => :overlaps)

;;How broad should we try to go? :starts / :finishes only?
(future-fact "Any pair of consistent, fully-defined intervals have a relation."
             (relation-gen
               {:starts (from-iso "2016")
                :finishes   (from-iso "2017")}
               {:meets  (from-iso "2015")
                :met-by (from-iso "2018")}) => :equal)


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

