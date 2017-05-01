(defproject pneumatic-tubes "0.2.0"
  :description "WebSocket based transport of events between re-frame app and server"
  :url "https://github.com/drapanjanas/pneumatic-tubes"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha16"]
                 [org.clojure/clojurescript "1.9.521"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/core.async "0.3.442"]
                 [com.cognitect/transit-cljs "0.8.239"]
                 [com.cognitect/transit-clj "0.8.300"]
                 [http-kit "2.2.0"]]

  :scm {:name "git"
        :url  "https://github.com/drapanjanas/pneumatic-tubes"}

  :min-lein-version "2.5.3"
  :source-paths ["src/clj"]
  :plugins [[lein-cljsbuild "1.1.4"]]
  :hooks [leiningen.cljsbuild]

  :cljsbuild {:builds [{:source-paths ["src/cljs"]
                        :compiler     {:output-to "target/pneumatic-tubes.js"}
                        :jar          true}]})
