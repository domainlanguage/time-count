(ns time-count.explainer.c-allens-interval-algebra
  (:require
    [time-count.core :refer :all]
    [time-count.allens-algebra :refer [relation]]
    [time-count.relation-bounded-intervals :refer [map->RelationBoundedInterval ->RelationBound relate-bound-to-ct]]
    [time-count.iso8601 :refer [to-iso from-iso t->>]]
    [time-count.metajoda]
    [midje.sweet :refer :all]))

;; In time-count, all time values are interpreted as intervals.
;; CountableTimes are taken to span period the length of their scale.
;; For example, 2017-12-13 is an interval 1 day long. 2017-12-13T09:15 is an interval 1 minute long.

;; Traditional "before/after" comparison is incomplete, and can result in ambiguous or complex business logic.
;; time-count uses Allen's Interval Algebra to compare intervals.
;; (There is a good explanation here https://www.ics.uci.edu/%7Ealspaugh/cls/shr/allen.html)
;; Allen defined 13 basic relations, which are distinct and exhaustive:
(fact "Thirteen basic relations in Allen's Interval Algebra"
      (t->> ["2017" "2017"] (apply relation)) => :equal
      (t->> ["2015" "2017"] (apply relation)) => :before
      (t->> ["2017" "2015"] (apply relation)) => :after
      (t->> ["2016" "2017"] (apply relation)) => :meets
      (t->> ["2017" "2016"] (apply relation)) => :met-by
      (t->> ["2017-01" "2017"] (apply relation)) => :starts
      (t->> ["2017" "2017-01"] (apply relation)) => :started-by
      (t->> ["2017-12" "2017"] (apply relation)) => :finishes
      (t->> ["2017" "2017-12"] (apply relation)) => :finished-by
      (t->> ["2017-02" "2017"] (apply relation)) => :during
      (t->> ["2017" "2017-02"] (apply relation)) => :contains
      (t->> ["2017-W05" "2017-02"] (apply relation)) => :overlaps
      (t->> ["2017-02" "2017-W05"] (apply relation)) => :overlapped-by)


;; Intervals other than those defined as countable times, can be described by their boundaries.
;; The boundary of an interval can be defined in terms of a relation to another interval.
;; In the current implementation of time-count, only :starts and :finishes are supported.

(fact "RelationBoundedIntervals are defined by a combination of Allen's :starts and :finishes relations to other intervals."
      (-> {:starts (from-iso "2017") :finishes (from-iso "2019")}
          map->RelationBoundedInterval
          to-iso)
      => "2017/2019"

      (-> {:starts (from-iso "2017-10") :finishes (from-iso "2017-12")}
          map->RelationBoundedInterval
          to-iso)
      => "2017-10/2017-12"

      (-> {:starts (from-iso "2017-10")}
          map->RelationBoundedInterval
          to-iso)
      => "2017-10/-")

(fact "RelationBoundedIntervals and CountableTimes are compared in the same way."
      (relation (from-iso "2017") (from-iso "2019"))
      => :before

      (relation (from-iso "2017/2018") (from-iso "2019/2020"))
      => :meets)

(fact "RelationBoundedIntervals and CountableTimes can be compared to each other."
      (relation (from-iso "2017/2019") (from-iso "2018"))
      => :contains

      (relation (from-iso "2018") (from-iso "2017/2019"))
      => :during)

(facts "About RelationBounds (A RelationBoundedInterval is just a combination of one or two boundaries.)"
       (fact "The relation of a single RelationBound with a CountableTime gives a set of possible basic relations (which is, itself, a relation)."
             (relate-bound-to-ct
               (->RelationBound :finishes (from-iso "2018"))
               (from-iso "2017"))
             => #{:overlapped-by :started-by :contains}))
;The basic relations of a RelationBoundedInterval are the intersection of the relations of its two bounds.


(fact "Business rules comparing time are expressed using sets of relations."
      (let [contract {:due-date (from-iso "2017-02-15")
                      :overdue-relation #{:after :met-by} ;This defines what it means to be 'overdue' in relation to the due-date
                      ; ... other contract stuff ...
                      }
            overdue-as-of? #((contract :overdue-relation) (relation % (contract :due-date)))]

        (overdue-as-of? (from-iso "2017-02-10T14:30")) => falsey
        (overdue-as-of? (from-iso "2017-02-15T20:30")) => falsey
        (overdue-as-of? (from-iso "2017-02-15T23:59")) => falsey
        (overdue-as-of? (from-iso "2017-02-16T00:00")) => truthy
        (overdue-as-of? (from-iso "2017-02-17T10:04")) => truthy)

      (let [contract {:active-interval (from-iso "2017-01-01/2017-03-31")
                      :active-relation #{:starts :during :finishes} ;This defines what it means to be 'active' in relation to the active-interval.
                      ; ... other contract stuff ...
                      }
            active-as-of? #((contract :active-relation) (relation % (contract :active-interval)))]

           (active-as-of? (from-iso "2016-12-10T14:30")) => falsey
            (active-as-of? (from-iso "2016-12-31T23:59")) => falsey
            (active-as-of? (from-iso "2017-02-15T23:59")) => truthy
            (active-as-of? (from-iso "2017-03-31T23:59")) => truthy
            (active-as-of? (from-iso "2017-04-01T00:00")) => falsey))
