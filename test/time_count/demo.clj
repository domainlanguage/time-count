(ns time-count.demo
  (:require [time-count.time-count :refer :all]
            [time-count.allens-interval-algebra :refer [relation]]
            [time-count.meta-joda :refer [stringify destringify]]
            [midje.sweet :refer :all])
  (:import [java.util.Date]))



(fact "time representation needs metadata representing nested scale"
      (-> "2017-04-09T11:17" destringify rest) => [:minute :hour :day :month :year]
      (-> "2017-04" destringify rest) => [:month :year])

(fact "an interval is part of a sequence, so next is meaningful"
      (let [next-str (t-> next-interval)]
        (next-str "2017-04-09") => "2017-04-10"
        (next-str "2017-04") => "2017-05"
        (next-str "2017") => "2018"
        (next-str "2017-04-09T11:17") => "2017-04-09T11:18"
        (next-str "2017-02-28") => "2017-03-01"
        (next-str "2016-02-28") => "2016-02-29"))

(fact "an interval sequence can be nested within an interval of a higher scale."
            (-> "2017-04-09" destringify ((enclosing)) stringify)
            => "2017-04"
            (-> "2017-04-09" destringify ((enclosing)) next-interval stringify)
            => "2017-05"
            ((t-> (enclosing) next-interval) "2017-04-09")
            => "2017-05")

(fact "example: is the invoice due"
      (let [net-30 (t-> interval-seq #(nth % 30))
            net-30-EOM (t-> (enclosing :month) next-interval (nested-last :day))
            overdue? (fn [terms completion-date today] (#{:after :met-by} (relation today (terms completion-date))))]

        (net-30 "2017-01-15") => "2017-02-14"
        (net-30-EOM "2017-01-15") => "2017-02-28"
        (overdue? net-30 "2017-01-15" "2017-02-10T14:30") => falsey
        (overdue? net-30 "2017-01-15" "2017-02-20" ) => truthy
        (overdue? net-30-EOM "2017-01-15" "2017-02-20" ) => falsey
        (overdue? net-30-EOM "2017-01-15" "2017-03-01") => truthy))




