(ns time-count.metajoda
  (:require [time-count.core :refer :all]                   ;[CountableTime next-t Interval ->RelationBoundedInterval]]
            [time-count.iso8601 :refer :all])

  (:import [org.joda.time LocalDateTime DateTimeZone
                          Years Months Weeks Days Hours Minutes Seconds]
           [org.joda.time.format DateTimeFormat]))


(def scale-to-Period
  {:year   (Years/years 1)
   :month  (Months/months 1)
   :day    (Days/days 1)
   :hour   (Hours/hours 1)
   :week   (Weeks/weeks 1)
   :minute (Minutes/minutes 1)})

(def joda-Property-for-nesting
  "These Property objects give access to JodaTime's logic of how scales fit together."
  {[:month :year]     (fn [dt] (.monthOfYear dt))
   [:day :month]      (fn [dt] (.dayOfMonth dt))
   [:hour :day]       (fn [dt] (.hourOfDay dt))
   [:minute :hour]    (fn [dt] (.minuteOfHour dt))
   [:day :year]       (fn [dt] (.dayOfYear dt))
   [:week :week-year] (fn [dt] (.weekOfWeekyear dt))
   [:day :week]       (fn [dt] (.dayOfWeek dt))
   [:year]            (fn [dt] (.year dt))
   [:week-year]       (fn [dt] (.weekyear dt))})


(defn- nested-first
  [{:keys [dt nesting]} nested-scale]
  (if-let [nesting-fn (joda-Property-for-nesting [nested-scale (first nesting)])]
    {:dt      (-> dt nesting-fn .withMinimumValue)
     :nesting (cons nested-scale nesting)}))

(defn- nested-last
  [{:keys [dt nesting]} nested-scale]
  (if-let [nesting-fn (joda-Property-for-nesting [nested-scale (first nesting)])]
    {:dt      (-> dt nesting-fn .withMaximumValue)
     :nesting (cons nested-scale nesting)}))

(defn- nesting-pairs [nesting]
  (concat
    (map vector nesting (rest nesting))
    [[(last nesting)]]))

(defn- place-value-for-pair [t scale-pair]
  (-> (:dt t) ((joda-Property-for-nesting scale-pair)) .get))

(def mapable-nestings
  [#{[:day :month :year]
     [:day :year]
     [:day :week :week-year]}
   #{}])

(defn mapable? [nesting1 nesting2]
  (some #(every? % [nesting1 nesting2]) mapable-nestings))


(defrecord MetaJodaTime [dt nesting]

  CountableTime

  (next-t [t]
    (MetaJodaTime.
      (.plus dt (scale-to-Period (first nesting)))
      nesting))

  (prev-t [t]
    (MetaJodaTime.
      (.minus dt (scale-to-Period (first nesting)))
      nesting))

  (nest [t scale]
    (let [{dts :dt n :nesting} (nested-first t scale)
          {dtf :dt} (nested-last t scale)]
      (if dts
        (->RelationBoundedInterval
          (MetaJodaTime. dts n)
          (MetaJodaTime. dtf n)))))

  (enclosing-immediate [t]
    ;Clearing insignificant places: JodaTime has a millisecond representation, whatever the scale in meta-joda.
    ;Mostly, this would be ignored, but the first operation below sets it to a default of the first millisecond
    ;within the interval.

    (let [{dt-with-insignificant-places-cleared :dt} (nested-first {:dt dt :nesting (rest nesting)} (first nesting))]
      (MetaJodaTime. dt-with-insignificant-places-cleared (rest nesting))))

  (place-values [t]
    (map #(vector (first %) (place-value-for-pair t %)) (nesting-pairs nesting)))

  (to-nesting [t scales]
    (if (mapable? nesting scales)
      (assoc t :nesting scales)
      :no-mapping))

  ISO8601Mappable

  (to-iso [t]
    (if (= org.joda.time.LocalDateTime (type dt))
      (-> nesting nesting-to-pattern DateTimeFormat/forPattern (.print dt))
      (let [p (nesting-to-pattern nesting)
            pZ (if (clojure.string/includes? p "'T'") "ZZ" "'T'ZZ")
            zone (.getZone dt)]
        (str
          (-> (str p pZ)
              DateTimeFormat/forPattern
              (.print dt))
          (if (= org.joda.time.tz.CachedDateTimeZone (type zone)) (str "[" zone "]"))))))

  )                                                         ;end defrecord

(extend-type String
  ISO8601Pattern
  (iso-parser [pattern]
    (fn [time-string]
      (let [[t-str offset-str zone-id] (tz-split time-string)
            t-unqualified (-> pattern
                              DateTimeFormat/forPattern
                              (.parseLocalDateTime t-str))
            t-maybe-offset (if offset-str
                             (let [[h m] (offset-parse offset-str)]
                               (.toDateTime t-unqualified (DateTimeZone/forOffsetHoursMinutes h m)))
                             t-unqualified)
            t-maybe-zone (if zone-id
                           (.toDateTime t-maybe-offset (DateTimeZone/forID zone-id))
                           t-maybe-offset)]
        (->MetaJodaTime t-maybe-zone (pattern-to-nesting pattern))))))


(extend-type String
  ISO8601CountableTime
  (from-iso-countable-time [time-string]
    ((-> time-string tz-split first time-string-pattern iso-parser) time-string)))

(defn nesting-from-place-values [place-vals]
  (map first place-vals))

(defn- nesting-pairs-with-vals [place-vals]
  (let [pairs (nesting-pairs (nesting-from-place-values place-vals))
        vals (map second place-vals)]
    (map #(assoc {} :pair %1 :val %2) pairs vals)))

(defn- set-place-vals-on-mjt [dt rev-place-val-pairs]
  (if (empty? rev-place-val-pairs)
    dt
    (let [pair (-> rev-place-val-pairs first :pair)
          val (-> rev-place-val-pairs first :val)
          p (joda-Property-for-nesting pair)
          reset-dt (.setCopy (p dt) val)]
      (recur reset-dt (rest rev-place-val-pairs)))))

(defn from-place-values [place-vals]                        ; TODO Consider making place-values a record and PlaceValueMappable a protocol like ISO8601Mappable.
  (MetaJodaTime.
    (set-place-vals-on-mjt
      (LocalDateTime. 1970, 1, 1, 0, 0, 0, 0)
      (-> place-vals nesting-pairs-with-vals reverse))
    (nesting-from-place-values place-vals)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;  Allen's Interval Algebra  ;;;
;;;  Basic relations           ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(extend-protocol Interval
  MetaJodaTime
  (relation [{dt-a :dt :as a} {dt-b :dt :as b}]
    (let [a-left dt-a
          {a-right :dt} (next-t a)
          b-left dt-b
          {b-right :dt} (next-t b)]
      (cond
        (and (.isEqual a-left b-left)
             (.isEqual a-right b-right)) :equal
        (.isBefore a-right b-left) :before
        (.isAfter a-left b-right) :after
        (.isEqual a-right b-left) :meets
        (.isEqual b-right a-left) :met-by
        (and (.isEqual a-left b-left)
             (.isBefore a-right b-right)) :starts
        (and (.isEqual a-left b-left)
             (.isAfter a-right b-right)) :started-by
        (and (.isEqual a-right b-right)
             (.isAfter a-left b-left)) :finishes
        (and (.isEqual a-right b-right)
             (.isBefore a-left b-left)) :finished-by
        (and (.isAfter a-left b-left)
             (.isBefore a-right b-right)) :during
        (and (.isBefore a-left b-left)
             (.isAfter a-right b-right)) :contains
        (and (.isBefore a-left b-left)
             (.isBefore a-right b-right)
             (.isAfter a-right b-left)) :overlaps
        (and (.isBefore b-left a-left)
             (.isBefore b-right a-right)
             (.isAfter b-right a-left)) :overlapped-by

        :else :TBD)
      )))



;(defn scale-pairs)
;(defn place-value [scale [date & nesting]]
;  {:pre [(some #{scale} nesting)]}
;  (let [scale-pair (->> nesting (drop-while #(not (= scale %))) (take 2))]
;    (-> date ((apply nesting-fns scale-pair)) .get)))