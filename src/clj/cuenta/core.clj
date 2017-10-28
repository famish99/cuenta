(ns cuenta.core
  (:require [bidi.bidi :as bidi]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [cognitect.transit :as transit]
            [ring.util.response :refer [not-found resource-response]]
            [cuenta.calc :as calc])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn load-state
  []
  (with-open [fp (-> "data_store.json" io/resource io/input-stream)]
    (->> (transit/reader fp :json)
         transit/read)))

(def app-db (atom (load-state)))

(defn swap-n-save!
  [f & args]
  (with-open [fp (-> "data_store.json" io/resource io/output-stream)]
    (let [writer (transit/writer fp :json)]
      (->> (apply swap! app-db f args)
           (transit/write writer))
      @app-db)))

(defn transit-decode
  [input-stream & {:keys [msg-type] :or {msg-type :json}}]
  (-> (transit/reader input-stream msg-type)
      transit/read))

(defn transit-encode
  [payload & {:keys [msg-type] :or {msg-type :json}}]
  (let [output-stream (ByteArrayOutputStream.)
        writer (transit/writer output-stream msg-type)]
    (transit/write writer payload)
    (ByteArrayInputStream. (.toByteArray output-stream))))

(defn parse-float [input] (if (string? input) (Double/parseDouble input) input))

(defn parse-int [input] (if (string? input) (Integer/parseInt input) input))

(def resource-routes
  ["/" {#{"" "index"} :index
        [[#"(css|js|static)" :resource-type]] {[[#".*" :resource-name]] :static-internal}
        "api/load/matrix" :api
        "api/save/transaction" :api
        "loopback" :loopback}])

(defn index-handler [request]
  "Handler for serving the base HTML"
  (resource-response "index.html"))

(defn int-handler [request]
  "Handler for the internal static resources"
  (if-let [response (-> (format "%s/%s"
                          (-> request :route-params :resource-type)
                          (-> request :route-params :resource-name))
                        (resource-response))]
    response
    (not-found "Resource not found")))

(defn not-found-handler [request]
  "Handler to return 404 responses"
  (not-found "Error 404: Could not find resource"))

(defn adjust-items
  [[key value-map]]
  [key (-> value-map
           (update :item-price parse-float)
           (update :item-quantity parse-int))])

(defn process-transaction
  [params]
  (-> params
      (update :items #(->> % (map adjust-items) (into {})))
      (update :tax-rate parse-float)
      (update :credit-to #(or % (-> params :people first)))
      (->> (swap-n-save! update :transactions conj))
      calc/process-transaction
      (->> (swap-n-save! assoc :owed-matrix))
      :owed-matrix))

(defn load-matrix [_] (Thread/sleep 100) (-> @app-db :owed-matrix))

(defn dump-db [_] @app-db)

(def route-map
  {:save-transaction process-transaction
   :load-matrix load-matrix
   :dump-db dump-db})

(defn api-handler
  [request]
  (let [params (transit-decode (:body request))
        handler (some->> params :action (get route-map))]
    (if handler
      {:status 200
       :headers {"Content-Type" "application/transit+json"
                 "Access-Control-Allow-Methods" "POST"
                 "Cache-Control" "no-cache, no-store, must-revalidate"
                 "Pragma" "no-cache"
                 "Expires" 0}
       :body (transit-encode (handler (-> params (dissoc :action))))}
      (not-found "Invalid API action"))))

(defn loopback-handler
  [request]
  {:status 200
   :headers {"Content-Type" "application/transit+json"}
   :body (:body request)})

(def backend-map
  {:index index-handler
   :static-internal int-handler
   :api api-handler
   :loopback loopback-handler})

(defn backend-handler
  [request]
  (println (:uri request) (bidi/match-route resource-routes (:uri request)))
  (if-let [{:keys [handler route-params]}
           (bidi/match-route resource-routes (:uri request))]
    (if-let [handler-f (handler backend-map)]
      (handler-f (assoc request :route-params route-params))
      ; else
      (index-handler request))
    (not-found-handler request)))
