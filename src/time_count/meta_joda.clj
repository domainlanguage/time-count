(ns time-count.meta-joda
  (:import [org.joda.time DateTime
                          Years Months Weeks Days Hours Minutes Seconds]))

(defn mj-time? [x]
  (and (sequential? x)
       (let [[^DateTime date & nesting] x]
         (and (-> date type (= DateTime)) (not-empty nesting)))))

(defn scale-to-Period [scale]
  (case scale
    :year (Years/years 1)
    :month (Months/months 1)
    :day (Days/days 1)
    :hour (Hours/hours 1)
    :week (Weeks/weeks 1)
    :minute (Minutes/minutes 1)
    :no-match))


(defn nesting-fns [& proposed-nesting]
  (case proposed-nesting
    [:month :year] (fn [^DateTime dt] (.monthOfYear dt))
    [:day :month] (fn [^DateTime dt] (.dayOfMonth dt))
    [:hour :day] (fn [^DateTime dt] (.hourOfDay dt))
    [:minute :hour] (fn [^DateTime dt] (.minuteOfHour dt))
    [:day :year] (fn [^DateTime dt] (.dayOfYear dt))
    [:week :week-year] (fn [^DateTime dt] (.weekOfWeekyear dt))
    [:day :week] (fn [^DateTime dt] (.dayOfWeek dt))
    [:year] (fn [^DateTime dt] (.year dt))
    [:week-year] (fn [^DateTime dt] (.weekyear dt))
    :no-match))




(def mapable-nestings
  [[:day :month :year]
   [:day :year]
   [:day :week :week-year]])

(defn to-nesting
  ;;TODO Validate that both in and out are mapable-nestings
  ([target-nesting [^DateTime date & nesting]] (cons date target-nesting))
  ([target-nesting] (partial to-nesting target-nesting)))



(defn place-value [scale [^DateTime date & nesting]]
  {:pre [(some #{scale} nesting)]}
  (let [scale-pair (->> nesting (drop-while #(not (= scale %))) (take 2))]
    (-> date ((apply nesting-fns scale-pair)) .get)))


(defn place-values [[_ & nesting :as mjt]]
  "Return a sequential representation, [:scale place-value :scale place-value]"
  ;TODO This has many unnecessary passes
  (interleave nesting
              (map #(place-value % mjt) nesting)))

(defn same-time?
  "meta-joda times can have non-zero values
  for scales smaller than those in the nesting,
  and they are ignored. However, this makes
  an = comparison to be unreliable.
  This comparison ignores insignificant scales."
  [mjt1 mjt2]
  (= (place-values mjt1) (place-values mjt2)))