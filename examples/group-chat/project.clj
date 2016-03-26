(defproject group-chat "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [reagent "0.5.1"]
                 [re-frame "0.7.0"]
                 [compojure "1.4.0"]
                 [ring "1.4.0"]
                 [ring/ring-defaults "0.2.0"]
                 [pneumatic-tubes "0.1.0-SNAPSHOT"]
                 [ch.qos.logback/logback-classic "1.1.1"]
                 [com.datomic/datomic-free "0.9.5350" :exclusions [org.slf4j/slf4j-nop joda-time org.slf4j/slf4j-log4j12]]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.0-6"]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler group-chat.core/app}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]
                        :figwheel {:on-jsload "group-chat.core/mount-root"}
                        :compiler {:main group-chat.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true}}

                       {:id "min"
                        :source-paths ["src/cljs"]
                        :compiler {:main group-chat.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :closure-defines {goog.DEBUG false}
                                   :pretty-print false}}]})
