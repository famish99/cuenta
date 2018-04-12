(ns backend
  (:require [cuenta.core :refer [backend-handler]]
            [immutant.web :as web]
            [ring.middleware.defaults :refer :all])
  (:gen-class))

(defn main
  [& args]
  (web/run backend-handler {:host "0.0.0.0"
                            :port (get (System/getenv) "SERVER_PORT" 3000)
                            :path "/"}))
