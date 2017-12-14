(ns time-count.explainer.d-composing-operations
  (:require [time-count.core :refer :all]
            [time-count.iso8601 :refer [from-iso t-> t->>]]
            [midje.sweet :refer :all]))

(fact "Two special threading macros let us use the concise and readable string representation (which is not convenient for computation)."
      ;; This may be mostly for tests and demos. Perhaps it will be used in some apps for data interchange.
      (t-> "2017-04-30" identity) => "2017-04-30"
      (t->> "2017-04-30" identity) => "2017-04-30")


(facts "about composing higher-level time operations from the basic protocol."
       (fact "A function to count forward is built on sequence operators.
              It could be partially applied to create a more specialized fn,
              relevant in a particular business rule"

             (letfn [(later [t n] (-> {:starts t} t-sequence (nth n)))]
               (t-> "2017-04-19" (later 5)) => "2017-04-24"
               (t-> "2017-04" (later 5)) => "2017-09"
               (t-> "2017" (later 5)) => "2022"
               (let [later5 #(later % 5)]
                 (t-> "2017" later5) => "2022")))

       (fact "An equivalent function to count forward, written in a different stlye.
              This style emphasizes partial application, perhaps to pass the fn
              around as a rule or part of a rule."
             (letfn [(later [n] (fn [t] (-> {:starts t} t-sequence (nth n))))]
               (t-> "2017-04-19" ((later 5))) => "2017-04-24"
               (t-> "2017-04" ((later 5))) => "2017-09"
               (t-> "2017" ((later 5))) => "2022"
               (let [later5 (later 5)]
                 (t-> "2017" later5) => "2022")))

       (fact "Business rules commonly refer to the last day of month, quarter or year.
              By composing the nesting navigation operations, we can make that."
             (letfn [(last-day [t] (-> t (nest :day) t-sequence last))]
               (t-> "2017-04" last-day) => "2017-04-30"
               (t-> "2017" last-day) => "2017-365"
               (t-> "2017-W02" last-day) => "2017-W02-7"))

       (fact "Alternative implementation,
              taking advantage of nest producing relation-bounded-interval."
             (letfn [(last-day [t] (-> t (nest :day) :finishes))]
               (t-> "2017-04" last-day) => "2017-04-30"
               (t-> "2017" last-day) => "2017-365"
               (t-> "2017-W02" last-day) => "2017-W02-7"))

       (fact "End-of-month is a common business operation, composed of seq and nest ops."
             (letfn [(eom [t] (-> t (enclosing :month) (nest :day) t-sequence last))]
               (t-> "2017-04-19" eom) => "2017-04-30"
               (t-> "2017-04" eom) => "2017-04-30"
               (t-> "2017-04-19T15:12" eom) => "2017-04-30"))
       ; A more complex eom could preserve nesting
       ; and find last interval of same scale, etc.

       (fact "The operations we create can be composed into higher ones also.
              E.g. End-of-month (eom) is a compositon of last-day and some nesting ops.
              A project might create their own library based on their business rules."
             (letfn [(last-day [t] (-> t (nest :day) t-sequence last))
                     (eom [t] (-> t (enclosing :month) last-day))]
               (t-> "2017-04-19" eom) => "2017-04-30"
               (t-> "2017-04" eom) => "2017-04-30"
               (t-> "2017-04-19T15:12" eom) => "2017-04-30"))

       (fact "Example: invoice due"
             (letfn [(overdue? [t due-date] (-> (relation t due-date) #{:after :met-by}))
                     (later [t n] (-> {:starts t} t-sequence (nth n)))
                     (last-day [t] (-> t (nest :day) t-sequence last))
                     (eom [t] (-> t (enclosing :month) last-day))
                     (net-30 [t] (-> t (enclosing :day) (later 30)))]
               (t-> "2017-01-15" net-30) => "2017-02-14"

               (let [work-completion (from-iso "2017-01-15T17:00")
                     t1 (from-iso "2017-02-10T11:10")
                     t2 (from-iso "2017-02-20T14:30")
                     t3 (from-iso "2017-03-01T09:15")]

                 (overdue? t1 (net-30 work-completion)) => falsey
                 (overdue? t2 (net-30 work-completion)) => truthy
                 (overdue? t3 (net-30 work-completion)) => truthy

                 (let [net-30-eom (comp eom net-30)]        ;compose different payment terms
                   (t-> "2017-01-15T17:00" net-30-eom) => "2017-02-28"
                   (overdue? t1 (net-30-eom work-completion)) => falsey
                   (overdue? t2 (net-30-eom work-completion)) => falsey
                   (overdue? t3 (net-30-eom work-completion)) => truthy)))))
