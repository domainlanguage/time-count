(ns time-count.relation-bounded-interval-tests
  (:require
    [time-count.core :refer [t-sequence t-rev-sequence]] ;See TODO comment by t-sequence function.
    [time-count.metajoda]
    [time-count.iso8601 :refer [from-iso]]
    [time-count.relation-bounded-intervals :refer :all]
    [midje.sweet :refer :all]))

; relation-bound
;   tuple of relation and an interval
;   e.g. [:starts 2017], meaning that, for some interval x, "2017 starts x"
;        [:ends 2017] means that, for some interval x, "2017 ends x"
; For exact bounds, only :starts, :ends, :meets, :met-by or :equals would really work.
; Other relations would constrain the interval, but wouldn't fully define it.

; Alternative concept (not used here currently): relation-spec
;   - same structure
;   - satisfies
;     e.g. (satisfies [:starts 2017] 2017-01) => true
;          (satisfies [:overlaps 2017] 2017-01) => false
;   - combine with AND

; relation-bounded interval
;   defined by one or two relation-bounds
;   to make it tractable, only :starts and :finishes
;   (might add :meets :met-by, a few others, later)
;   e.g. {:starts 2017}
;   - fully-specified (does it define a single interval?)
;   e.g. {:starts 2017 :finishes 2018}
;
;
;  TODO What are the compatibility rules for the two bounds? (e.g. same nesting, mappable nesting ...)
;   To make it less confusing, perhaps both bounds should be same scale. That will be the scale of the relation-bounded interval.
;   - (scale {:starts 2017 :finishes 2018}) => :year
;   This would make the t-sequence unambiguous -- it would be a sequence starting with the :starts bound ...
;   - t-sequence (requires :starts, :finishes is optional,
;         although interval-last could still have a value with :finishes and not :starts)
;   Requiring :starts and :ends to have the same scale makes nest on a relation-bounded-interval clearer.
;   TODO? Write a 'nest' function for rbi?
;   e.g. ((nest :month) {:starts 2017 :finishes 2018}) => {:starts 2017-01 :finishes 2018-12}
;   (enclosing-immediate {:starts 2017-03 :finishes 2018-03}) => {:starts 2017 :finishes 2018} ???
;   (enclosing-immediate {:starts 2017-01 :finishes 2017-05} => 2017 (CountableTime) ???
;

;; Boundary relations may not bound both sides.
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

(fact "A relation-bounded interval defines a sequence of CountableTimes"

      (t-sequence (map->RelationBoundedInterval
                    {:starts (from-iso "2016-12-31")
                     :finishes (from-iso "2017-01-02")}))
      => [(from-iso "2016-12-31")
          (from-iso "2017-01-01")
          (from-iso "2017-01-02")]

      ;t-sequence treats it as a map, so it is the same as this
      (t-sequence {:starts (from-iso "2016-12-31")
                   :finishes (from-iso "2017-01-02")})
      => [(from-iso "2016-12-31")
          (from-iso "2017-01-01")
          (from-iso "2017-01-02")]

      (t-rev-sequence (map->RelationBoundedInterval
                        {:starts (from-iso "2016-12-31")
                         :finishes (from-iso "2017-01-02")}))
      => [(from-iso "2017-01-02")
          (from-iso "2017-01-01")
          (from-iso "2016-12-31")]


      (take 3 (t-sequence (map->RelationBoundedInterval
                            {:starts (from-iso "2016-12-31")})))
      => [(from-iso "2016-12-31")
          (from-iso "2017-01-01")
          (from-iso "2017-01-02")]

      (take 3 (t-rev-sequence (map->RelationBoundedInterval
                                {:finishes (from-iso "2017-01-02")})))
      => [(from-iso "2017-01-02")
          (from-iso "2017-01-01")
          (from-iso "2016-12-31")])


(fact "Boundary relations must be consistent for all this to work!"
      (consistent? (from-iso "2016/2018")) => truthy
      (consistent? (from-iso "2019/2016")) => falsey
      (consistent? (from-iso "2016/2017-06")) => truthy
      (consistent? (from-iso "2016/2016-06")) => falsey
      (consistent? (from-iso "2017/-")) => truthy
      (consistent? (from-iso "-/2017")) => truthy)

;; This next one is just an experiment with test-writing, and
;; doesn't add anything to understanding time-count
(fact "BTW Any predicate can be used as a checking function in midje.
       Should I rewrite some of the tests this way?"
      (from-iso "2016/2018") => consistent?
      (from-iso "2019/2016") => (comp not consistent?))
;; OK, back to the chase.

(fact "So long as we restrict bounds to :starts and :finishes, deep compositions can be flattened to equivalent intervals."
      (flatten-bounds (->RelationBoundedInterval
                        (->RelationBoundedInterval
                          (from-iso "2017-06")
                          (from-iso "2017-07/2017-08"))
                        (from-iso "2017-07/2017-10")))
      => (from-iso "2017-06/2017-10"))

(fact "With boundaries restricted to starts/ends, deep compositions can be flattened"

      ;TODO How to verify consistency of compositions?
      (flatten-bounds {:starts
                       {:starts
                        {:starts (from-iso "2017-06") :finishes
                                 {:starts (from-iso "2017-07") :finishes (from-iso "2017-08")}}}
                       :finishes
                       {:starts (from-iso "2017-07") :finishes (from-iso "2017-10")}})
      => {:starts (from-iso "2017-06") :finishes (from-iso "2017-10")})

;;Right now, only :starts/:finishes are supported. :meets/:met-by should also be tractable.
(future-fact "Any pair of consistent, fully-defined intervals have a relation."
             (relation
               {:starts   (from-iso "2016")
                :finishes (from-iso "2017")}
               {:meets  (from-iso "2015")
                :met-by (from-iso "2018")}) => :equal)


(fact "A single RelationBound has a relation to a CountableTime.
       Such an indefinite interval has a 'general relation'
       to the other interval, a set of possible relations,
       rather than the 'basic relation' of two definite intervals."

      (relate-bound-to-ct
        (->RelationBound :starts (from-iso "2015"))
        (from-iso "2017"))
      => #{:before :overlaps :contains :finished-by :meets}

      (relate-bound-to-ct
        (->RelationBound :finishes (from-iso "2018"))
        (from-iso "2017"))
      => #{:overlapped-by :started-by :contains}

      (relate-bound-to-ct
        (->RelationBound :meets (from-iso "2014"))
        (from-iso "2017"))
      => #{:before :overlaps :contains :finished-by :meets}

      (relate-bound-to-ct
        (->RelationBound :met-by (from-iso "2019"))
        (from-iso "2017"))
      => #{:after :met-by :overlapped-by :started-by :contains})


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

