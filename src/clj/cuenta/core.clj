(ns cuenta.core
  (:require [clojure.pprint :refer [pprint]]
            [ring.util.response :refer [not-found resource-response]]))

(defn backend-handler
  [request]
  (pprint request)
  (if (= (:uri request) "/api")
    {:status 200}
    {:status 404}))