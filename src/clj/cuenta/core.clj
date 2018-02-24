(ns cuenta.core
  (:require [bidi.bidi :as bidi]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [cognitect.transit :as transit]
            [ring.util.mime-type :as mime]
            [ring.util.response :as ring-r]
            [cuenta.calc :as calc]
            [cuenta.db :as db]
            [cuenta.db.transactions :as t]
            [cuenta.routes :as routes])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn transit-decode
  [input-stream & {:keys [msg-type] :or {msg-type :json}}]
  (when (> (.available input-stream) 0)
    (-> input-stream
        (transit/reader msg-type)
        transit/read)))

(defn transit-encode
  [payload & {:keys [msg-type] :or {msg-type :json}}]
  (let [output-stream (ByteArrayOutputStream.)
        writer (transit/writer output-stream msg-type)]
    (transit/write writer payload)
    (ByteArrayInputStream. (.toByteArray output-stream))))

(defn index-handler [_]
  "Handler for serving the base HTML"
  (-> (ring-r/resource-response "index.html")
      (assoc-in [:headers "Content-Type"] "text/html; charset-utf-8")))

(defn int-handler [request]
  "Handler for the internal static resources"
  (let [resource-name (-> request :route-params :resource-name)
        mime-type (mime/ext-mime-type resource-name)]
    (if-let [response (-> (format "%s/%s"
                                  (-> request :route-params :resource-type)
                                  resource-name)
                          (ring-r/resource-response)
                          (assoc-in [:headers "Content-Type"]
                                    (format "%s; charset=utf-8" mime-type)))]
      response
      (ring-r/not-found "Resource not found"))))

(defn not-found-handler [_]
  "Handler to return 404 responses"
  (ring-r/not-found "Error 404: Could not find resource"))

(defn adjust-items
  [[key value-map]]
  {key (-> value-map
           (update :item-price calc/cast-float)
           (update :item-quantity calc/cast-int))})

(defn process-transaction
  [tx params]
  (-> params
      (update :items #(->> % (map adjust-items) (into {})))
      (update :tax-rate calc/cast-float)
      (update :tip-amount calc/cast-float)
      (update :credit-to #(or % (-> params :people first)))
      (->> (t/process-transaction tx))))

(defn gen-api-handler
  [handler]
  (fn [request]
    {:status 200
     :headers {"Content-Type" "application/transit+json"
               "Access-Control-Allow-Methods" "POST"
               "Cache-Control" "no-cache, no-store, must-revalidate"
               "Pragma" "no-cache"
               "Expires" 0}
     :body (jdbc/with-db-transaction
             [tx db/db-spec]
             (db/clear-transaction-cache!)
             (->> request
                  :body
                  transit-decode
                  (handler tx)
                  transit-encode))}))

(defn loopback-handler
  [request]
  {:status 200
   :headers {"Content-Type" "application/transit+json; charset=utf-8"}
   :body (:body request)})

(def backend-map
  {:index index-handler
   :static-internal int-handler
   :load-transactions (gen-api-handler t/find-transactions)
   :save-transaction (gen-api-handler process-transaction)
   :load-matrix (gen-api-handler t/find-debt)
   :loopback loopback-handler})

(defn backend-handler
  [request]
  (log/info (:uri request))
  (if-let [{:keys [handler route-params]}
           (bidi/match-route routes/route-map (:uri request))]
    (if-let [handler-f (handler backend-map)]
      (handler-f (assoc request :route-params route-params))
      ; else
      (index-handler request))
    (not-found-handler request)))
