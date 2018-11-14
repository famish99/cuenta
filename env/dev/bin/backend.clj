(ns bin.backend
  (:require [cuenta.core :refer [backend-handler static-handler]]
            [immutant.web :as web]
            [ring.middleware.defaults :refer :all])
  (:gen-class))

(defn main [& args]
  (def server
    (-> {:host "0.0.0.0" :port (get (System/getenv) "SERVER_PORT" 3000)}
        (assoc :path "/js")
        (->> (web/run static-handler))
        (assoc :path "/css")
        (->> (web/run static-handler))
        (assoc :path "/")
        (->> (web/run-dmc backend-handler)))))
