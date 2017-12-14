(ns time-count.explainer.e-timezones
  (:require [time-count.core :refer :all]
            [time-count.iso8601 :refer [t-> t->>]]
            [time-count.metajoda]                           ;must have some implementation
            [midje.sweet :refer :all]))

(facts "about time zones"

       (fact "Each UTC offset and timezone is a distinct sequence."
             (t-> "2017-04-09T11:17" next-t) => "2017-04-09T11:18"
             (t-> "2017-04-09T11:17-04:00" next-t) => "2017-04-09T11:18-04:00"
             (t-> "2017-04-09T11:17-04:00[America/New_York]" next-t) => "2017-04-09T11:18-04:00[America/New_York]")

       (fact "Daylight savings time shifts are part of some sequences."
             (t-> "2017-11-05T01:59-04:00[America/New_York]" next-t) => "2017-11-05T01:00-05:00[America/New_York]"
             (t-> "2017-11-05T01-04:00[America/New_York]" next-t) => "2017-11-05T01-05:00[America/New_York]"
             (t-> "2017-11-05T01:59-04:00" next-t) => "2017-11-05T02:00-04:00")

       (future-fact "Even a date or year might need to be in a timezone for some needs. The 'T' must be included in the string."
                    ;TODO Does this make sense? A date, or anything above an hour, could be partly in one offset and partly in another!
                    (t-> "2017-11-05T-04:00[America/New_York]" next-t) => "2017-11-05T-05:00[America/New_York]"))