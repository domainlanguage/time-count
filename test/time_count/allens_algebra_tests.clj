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
         ))
        ; (relation-gen {:starts (iso-to-mj "2017") :finishes (iso-to-mj "2020")}
        ;               {:starts (iso-to-mj "2019") :finishes (iso-to-mj "2021")}) => :overlaps
        ; (relation-gen (iso-to-mj "2017")
        ;               {:starts (iso-to-mj "2020") :finishes (iso-to-mj "2021")}) => :before))