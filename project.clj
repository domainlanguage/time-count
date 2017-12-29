(defproject time-count "0.1.0-SNAPSHOT"
  :description "An alternative model of time for business applications, in the spirit of exploration."
  :url "http://example.com/FIXME"
  :license {:name "Apache License 2.0"
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [joda-time "2.9.9"]
                 [midje "1.8.3" :exclusions [org.clojure/clojure]]]
  :profiles {:dev {:dependencies [[midje "1.8.3" :exclusions [org.clojure/clojure]]]
                   :plugins      [[lein-midje "3.2"]]}}
  :source-paths ["src" "sandboxes"]
  :test-paths ["test" "explainers"])
