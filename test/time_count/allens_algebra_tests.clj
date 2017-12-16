(ns time-count.allens-algebra-tests
  (:require [midje.sweet :refer :all]
            [time-count.core :refer :all]                   ;TODO move relation to separate protocol?
            [time-count.metajoda]                           ; Must have some implementation!
            [time-count.iso8601 :refer [from-iso t-> t->>]]))

(facts "about Allen's Interval Algebra"
       (fact "Defines a relation between two intervals"
             (relation (from-iso "2017-03-10") (from-iso "2017-03-15")) => :before)

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

       (facts "about inverse relations"
              (fact "Each basic relation has an inverse"
                    (inverse-relation :equal) => :equal
                    (inverse-relation :before) => :after
                    ; etc.
                    (inverse-relation :equal) => :equal)
              (fact "Reversing the order of the args produces the inverse relation"
                    (t->> ["2015" "2017"] (apply relation) inverse-relation)
                    => (t->> ["2017" "2015"] (apply relation))))

       (fact "Different scales with same nesting can always be related."
             (t->> ["2016-12" "2017"] (apply relation)) => :meets
             (t->> ["2016-11" "2017"] (apply relation)) => :before)

       (fact "If nestings are different, but mappable, relation can be calculated."
             (t->> ["2017-221" "2017-08-09"] (apply relation)) => :equal
             (t->> ["2017-W32" "2017-08"] (apply relation)) => :during
             (t->> ["2017-W32-3" "2017-08-09"] (apply relation)) => :equal))


(facts "about relation-bounded intervals"
       (fact
         (relation (from-iso "2017/2018") (from-iso "2020/2021")) => :before
         (relation (from-iso "2017/2018") (from-iso "2017/2021")) => :starts)

       (fact "We can relate a CountableTime to a single bound"
             (relation-bound-starts (from-iso "2017") (from-iso "2019")) => #{:contains :finished-by :overlaps :meets :before}
             (relation-bound-finishes (from-iso "2021") (from-iso "2019")) => #{:met-by :contains :after :overlapped-by :started-by})

       (fact "We can calculate the relation between a RelationBoundedIntervals and a CountableTime."
             (relation (from-iso "2017/2018") (from-iso "2016")) => :met-by
             (relation (from-iso "2017/2018") (from-iso "2017")) => :started-by
             (relation (from-iso "2017/2018") (from-iso "2017-01")) => :started-by
             (relation (from-iso "2017-01/2018-12") (from-iso "2017")) => :started-by
             (relation (from-iso "2017/2018") (from-iso "2018-12")) => :finished-by
             (relation (from-iso "2017/2018") (from-iso "2018")) => :finished-by
             (relation (from-iso "2017-01/2018-12") (from-iso "2018")) => :finished-by
             (relation (from-iso "2017/2018") (from-iso "2015")) => :after)

       (fact "The relation between RelationBoundedInterval and a CountableTime works in either order."
             (relation (from-iso "2017") (from-iso "2017/2018")) => :starts
             (relation (from-iso "2017/2018") (from-iso "2017")) => :started-by))
