(defproject time-count "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [joda-time "2.9.9"]
                 [midje "1.8.3" :exclusions [org.clojure/clojure]]]
  :profiles {:dev {:dependencies [[midje "1.8.3" :exclusions [org.clojure/clojure]]]
                   :plugins      [[lein-midje "3.2"]]}}
  :source-paths ["src" "sandboxes"]
  :test-paths ["test" "explainers"])
