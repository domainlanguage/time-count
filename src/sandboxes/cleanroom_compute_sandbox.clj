(ns sandboxes.cleanroom-compute-sandbox
  (:require [midje.sweet :refer :all]))

(defn clean-up-increments [{increments :increments :as full-map}]
  (let [no-zeros (remove #(= 0 (val %)) increments)]
  (if (empty? no-zeros)
    (dissoc full-map :increments)
    full-map)))

(defn advance [date-map date-increments]
  (let [{{y :year} :date} date-map
        {y-advance :years} date-increments]
  (if (and y y-advance)
    (assoc-in date-map [:date :year] (+ y y-advance))
    date-map)))

(defn reduce-years [date-map]
              (let [{{y :year} :date {y-advance :years} :increments} date-map]
                (if (and y y-advance)
                  (-> date-map
                      (assoc-in [:date :year] (+ y y-advance))
                      (update-in [:increments] dissoc :years)
                      clean-up-increments)
                  date-map)))

(defn reduce-months [date-map]
  (let [{{m :month} :date {m-advance :months y-advance :years :or {y-advance 0}} :increments} date-map]
    (if (and m m-advance)
      (let [m-inc (rem m-advance 12)
            y-inc (unchecked-divide-int m-advance 12)]
        (-> date-map
            (assoc-in [:date :month] (+ m m-inc))
            (assoc-in [:increments :years] y-inc)
            (update-in [:increments] dissoc :months)
            clean-up-increments))
      date-map)))

(defn reduce-days [date-map]
  (let [{{m :month d :day} :date {d-advance :days m-advance :months :or {m-advance 0}} :increments} date-map]
    (println "m " m " d " d " d-advance " d-advance)
    (if (and m d d-advance)
      (let [m-length 30 ;; This should be a lookup
            d-tot (+ d-advance d)
            d-new (if (> d-tot m-length) (- d-tot m-length))
            m-inc (if (+ d-advance) 0)
            m-inc (unchecked-divide-int d-advance 30)]

        (-> date-map
            (assoc-in [:date :day] d-new)
            (assoc-in [:increments :months] m-inc)
            (update-in [:increments] dissoc :days)
            clean-up-increments))
      date-map)))

(fact "Adding a year to a date is simple because it is an unbounded number-line."
      (reduce-years {:date {:year 2017 :month 1 :day 5} :increments {:years 1}} )
      => {:date {:year 2018 :month 1 :day 5}}
      (reduce-years {:date {:year 2017 :month 1 :day 5} :increments {:years 1000}})
      => {:date {:year 3017 :month 1 :day 5}})

(fact "Elements not understood by a function are left undisturbed"
      (reduce-years {:date {:year 2017 :month 1 :day 5} :increments {:years 1 :foo 1}} )
      => {:date {:year 2018 :month 1 :day 5} :increments {:foo 1}})

(fact "Adding months is simple in some cases"
      (reduce-months {:date {:year 2017 :month 1 :day 5} :increments {:months 3}} )
      => {:date {:year 2017 :month 4 :day 5}})

(fact "Past month 12, we roll over years."
      (reduce-months {:date {:year 2017 :month 1 :day 5} :increments {:months 18}} )
      => {:date {:year 2017 :month 7 :day 5} :increments {:years 1}}
      ((comp reduce-years reduce-months) {:date {:year 2017 :month 1 :day 5} :increments {:months 18}})
      => {:date {:year 2018 :month 7 :day 5}})

(future-fact "When months have different lengths, rollover to the closest match within the target month (JodaTime's behavior)"
             (reduce-months {:date {:year 2017 :month 3 :day 31} :increments {:months 1}} )
             => {:date {:year 2017 :month 4 :day 30}}
             (reduce-months {:date {:year 2017 :month 4 :day 30} :increments {:months -1}} )
             => {:date {:year 2017 :month 3 :day 30}})


(fact "Adding days is simple if we don't roll over."
      (reduce-days {:date {:year 2017 :month 1 :day 5} :increments {:days 2}})
      => {:date {:year 2017 :month 1 :day 7}}
      )

(fact "Days rolling over to months depends on the length of the month."
      (reduce-days {:date {:year 2017 :month 6 :day 29} :increments {:days 5}})
      => {:date {:year 2017 :month 6 :day 4} :increments {:months 1}}
      )

(future-fact "Adding days and months to dates is trickier"
             (advance {:date {:year 2017 :month 1 :day 5}} {:days 30})
             => {:date {:year 2017 :month 2 :day 4}}

             (advance {:date {:year 2017 :month 1 :day 5}} {:months 1})
             => {:date {:year 2017 :month 2 :day 5}}
             (advance {:date {:year 2017 :month 1 :day 5}} {:months 14})
             => {:date {:year 2018 :month 4 :day 5}}

             )

