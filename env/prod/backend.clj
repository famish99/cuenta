(ns backend
  (:require [cuenta.core :refer [backend-handler]]
            [immutant.web :as web]
            [clojure.tools.logging :as log]
            [ring.middleware.defaults :refer :all])
  (:gen-class))

(defn main
  [& args]
  (let [port (get (System/getenv) "SERVER_PORT" 3000)]
    (log/info "Opening server on port:" port)
    (web/run backend-handler {:host "0.0.0.0" :port port :path "/"})))
