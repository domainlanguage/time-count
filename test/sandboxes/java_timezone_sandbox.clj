(ns sandboxes.java-timezone-sandbox
  (:import [java.time ZoneId ZoneOffset]))


(comment "Working with time-zones and non-timezones"

         (def aZ (->> (ZoneId/getAvailableZoneIds) (filter #(clojure.string/starts-with? % "America"))))

         ; ZoneOffset offsetInEffectNow = z.getRules().getOffset( Instant.now() );

         ;(-> (ZoneId/of "America/New_York") type)
         ;=> java.time.ZoneRegion

         (java.time.Instant/now)
         (java.time.Instant/parse "2017-09-25T20:10:05Z")

         ;https://stackoverflow.com/questions/44569202/java-8-create-instant-from-localdatetime-with-timezone
         ; LocalDateTime dateTime = LocalDateTime.of(2017, Month.JUNE, 15, 13, 39);
         ; Instant instant = dateTime.atZone(ZoneId.of("Europe/Paris")).toInstant();
         ;System.out.println(instant); // 2017-06-15T11:39:00Z

         (-> (java.time.LocalDateTime/of 2017 10 9 12 33 7))
         ;=> #object[java.time.LocalDateTime 0x3254125e "2017-10-09T12:33:07"]
         (-> (java.time.LocalDateTime/of 2017 10 9 12 33 7 13))
         ;=> #object[java.time.LocalDateTime 0x7b2a9c96 "2017-10-09T12:33:07.000000013"]

         (-> (java.time.LocalDateTime/of 2017 10 9 12 33) (.atZone (ZoneId/of "America/New_York")))
         ;=> #object[java.time.ZonedDateTime 0x63a3fe36 "2017-10-09T12:33-04:00[America/New_York]"]
         (-> (java.time.LocalDateTime/of 2017 10 9 12 33) (.atZone (ZoneId/of "America/New_York")) type)
         ;=> java.time.ZonedDateTime


         ;http://www.java2s.com/Tutorials/Java/java.time/ZonedDateTime/index.htm
         ;ZonedDateTime dateTime = ZonedDateTime.parse("2007-12-03T10:15:30+01:00[Europe/Paris]")

         (java.time.ZonedDateTime/parse "2017-11-05T02:01-05:00[America/New_York]")
         ;=> #object[java.time.ZonedDateTime 0x57db833 "2017-11-05T02:01-05:00[America/New_York]"]
         ; ROUND-TRIP string! Way to go java.time :-)

         (-> (java.time.ZonedDateTime/parse "2017-11-05T01:59-04:00[America/New_York]") .toInstant)
         ;=> #object[java.time.Instant 0x2b3aab23 "2017-11-05T05:59:00Z"]
         (-> (java.time.ZonedDateTime/parse "2017-11-05T01:01-05:00[America/New_York]") .toInstant)
         ;=> #object[java.time.Instant 0x258c6164 "2017-11-05T05:01:00Z"]

         (-> (java.time.ZonedDateTime/parse "2017-11-05T01:59-04:00[America/New_York]") (.plusMinutes 2))
         ;=> #object[java.time.ZonedDateTime 0x1f32437d "2017-11-05T01:01-05:00[America/New_York]"]

         (-> (java.time.ZonedDateTime/parse "2017-11-05T01:59-04:00[America/New_York]") .toInstant)
         ;=> #object[java.time.Instant 0x1504b1e4 "2017-11-05T05:59:00Z"]
         (-> (java.time.ZonedDateTime/parse "2017-11-05T01:59-04:00[America/New_York]") (.plusMinutes 2) .toInstant)
         ;=> #object[java.time.Instant 0x59ae9b92 "2017-11-05T06:01:00Z"]

         ;; YIKES! A BUG?!  https://bugs.openjdk.java.net/browse/JDK-8066982
         (-> (java.time.ZonedDateTime/parse "2017-11-05T01:01-05:00[America/New_York]"))
         ;=> #object[java.time.ZonedDateTime 0x314ef16a "2017-11-05T01:01-04:00[America/New_York]"]
         ;This bug is supposedly fixed in Java9,
         ; but I'm already pushing it requiring Java8 :-(


         ;Now, trying to reproduce MetaJoda with this...
         (java.time.temporal.IsoFields/QUARTER_OF_YEAR)
         ;=> #object[java.time.temporal.IsoFields$Field$2 0x706f5c4c "QuarterOfYear"]
         (java.time.temporal.ChronoField/MONTH_OF_YEAR)
         ;=> #object[java.time.temporal.ChronoField 0x71c8da41 "MonthOfYear"]

         ;let m be MonthOfYear
         (-> (java.time.ZonedDateTime/parse "2017-11-05T02:01-05:00[America/New_York]") (.range m))
         ;=> #object[java.time.temporal.ValueRange 0x6b670fa "1 - 12"]

         ;let dom be DayOfMonth

         ;get min and max
         (-> (java.time.ZonedDateTime/parse "2017-02-05T02:01-05:00[America/New_York]") (.range dom))
         ;=> #object[java.time.temporal.ValueRange 0x1e5962ad "1 - 28"]
         ; add days, designating field
         (-> (java.time.ZonedDateTime/parse "2017-02-05T02:01-05:00[America/New_York]") (.plus 2 (.getBaseUnit dom)))
         ;=> #object[java.time.ZonedDateTime 0x77bcd532 "2017-02-07T02:01-05:00[America/New_York]"]

         ;So... Looks like this would work,
         ; but might need Java9 to avoid bugs

         ; BTW, parsing controlled by patterns is a little different

         ;Doesn't fix the bug with parsing during DST change
         (java.time.ZonedDateTime/parse tstr2 f)
         ;=> #object[java.time.ZonedDateTime 0x628eb970 "2017-11-05T01:01-04:00[America/New_York]"]


         ;;SORT THIS STUFF
         (.parse f "2017-11-05T01:01-05:00[America/New_York]")
         ;=>
         ;#object[java.time.format.Parsed
         ;        0x351e300a
         ;        "{OffsetSeconds=-18000, InstantSeconds=1509858060},ISO,America/New_York resolved to 2017-11-05T01:01"]

         (def tstr2 "2017-11-05T01:01-05:00[America/New_York]")
         ;=> #'time-count.timezone-sandbox/tstr2
         (java.time.ZonedDateTime/parse tstr2)
         ;=> #object[java.time.ZonedDateTime 0x7288a87f "2017-11-05T01:01-04:00[America/New_York]"]
         (java.time.ZonedDateTime/parse f tstr2)
         ;ClassCastException java.time.format.DateTimeFormatter cannot be cast to java.lang.CharSequence  time-count.timezone-sandbox/eval2288 (form-init9139042811420978108.clj:1)
         (java.time.ZonedDateTime/parse tstr2 f)
         ;=> #object[java.time.ZonedDateTime 0x628eb970 "2017-11-05T01:01-04:00[America/New_York]"]

         )