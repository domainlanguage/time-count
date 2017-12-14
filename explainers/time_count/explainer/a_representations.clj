(ns time-count.explainer.a-representations
  (:require
    [time-count.iso8601 :refer [to-iso from-iso t-> t->>]]
    [time-count.metajoda :refer [map->MetaJodaTime from-place-values]]
    [time-count.core :refer [place-values]]
    [midje.sweet :refer :all])
  (:import [org.joda.time DateTime LocalDateTime DateTimeZone]))

;; Each time value in time-count must have at least two representations,
;; a string representation, usually from a subset of ISO8601,
;; and a representation that enables the basic protocols in time-count.core,
;; in particular, the CountableTime protocol.
;; MetaJodaTime (the provided default implementation) uses
;; a few types of DateTime from the Joda Time library,
;; combined with some metadata about nested scales.
;; Some other representation could be used, so long as it could
;; implement CountableTime and the other interfaces MetaJodaTime does.

(fact "The default representation of time values (for doing computations) is a record containing Joda Time DateTime values with added metadata for nested scales."
      (from-iso "2017-04-09")
      => (map->MetaJodaTime
           {:dt      (LocalDateTime. 2017 4 9 0 0 0 0)
            :nesting [:day :month :year]})

      (from-iso "2017-12-13T11:17")
      => (map->MetaJodaTime
           {:dt      (LocalDateTime. 2017 12 13 11 17 0 0)
            :nesting [:minute :hour :day :month :year]})

      (from-iso "2017-12-13T11:17-07:00")
      => (map->MetaJodaTime
           {:dt      (DateTime. 2017 12 13 11 17 0 0
                                (DateTimeZone/forOffsetHours -7))
            :nesting [:minute :hour :day :month :year]}))

(fact "All time values in time-count have a canonical string representation, based closely on ISO 8601, fully reversable from the MetaJoda representation."
      (from-iso "2017-04-09")
      => (map->MetaJodaTime
           {:dt      (LocalDateTime. 2017 4 9 0 0 0 0)
            :nesting [:day :month :year]})

      (to-iso (map->MetaJodaTime
                {:dt      (LocalDateTime. 2017 4 9 0 0 0 0)
                 :nesting [:day :month :year]}))
      => "2017-04-09")

(fact "A third representation is the place-values vector."
      ;place-values is currently a bit undeveloped, but it could be completely reversible with the other two, and maybe should be.
      (-> "2017-04-09" from-iso place-values)
      => [[:day 9] [:month 4] [:year 2017]]
      (-> [[:day 9] [:month 4] [:year 2017]] from-place-values to-iso)
      => "2017-04-09")

(fact "For convenience, two special threading macros allow use of ISO 8601 string representation with other operations."
      (t-> "2017-12-13" identity) ; Where, instead of identity, you could have any time operation.
      => (-> "2017-12-13" from-iso identity to-iso)

      (t->> "2017-12-13" identity)
      => (->> "2017-12-13" from-iso identity to-iso))


