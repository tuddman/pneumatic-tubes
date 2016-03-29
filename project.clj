(defproject pneumatic-tubes "0.1.0-SNAPSHOT"
  :description "WebSocket based transport of events between re-frame app and server"
  :url "https://github.com/drapanjanas/pneumatic-tubes"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/tools.logging "0.2.3"]
                 [org.clojure/core.async "0.2.374"]
                 [com.cognitect/transit-cljs "0.8.237"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [http-kit "2.1.18"]]

  :min-lein-version "2.5.3"
  :source-paths ["src/clj"]
  :plugins [[lein-cljsbuild "1.1.2"]]
  :hooks [leiningen.cljsbuild]

  :cljsbuild {:builds [{:source-paths ["src/cljs"]
                        :compiler     {:output-to "target/pneumatic-tubes.js"}
                        :jar          true}]})
