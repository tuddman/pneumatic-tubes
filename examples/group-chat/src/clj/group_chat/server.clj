(ns group-chat.server
  (:use org.httpkit.server)
  (:require [group-chat.core :refer [app]]
            [environ.core :refer [env]])
  (:gen-class))

(defn -main [& args]
  (let [port (Integer/parseInt (or (env :port) "3000"))]
    (run-server app {:port port :join? false})))