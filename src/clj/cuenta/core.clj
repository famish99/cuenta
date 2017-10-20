(ns cuenta.core
  (:require [bidi.bidi :as bidi]
            [clojure.pprint :refer [pprint]]
            [cognitect.transit :as transit]
            [ring.util.response :refer [not-found resource-response]]
            [cuenta.calc :as calc])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(def app-db (atom {}))

(defn parse-float [input] (if (string? input) (Double/parseDouble input) input))

(defn parse-int [input] (if (string? input) (Integer/parseInt input) input))

(def resource-routes
  ["/" {#{"" "index"} :index
        [[#"(css|js|static)" :resource-type]] {[[#".*" :resource-name]] :static-internal}
        "api" :api
        "loopback" :loopback}])

(defn index-handler [request]
  "Handler for serving the base HTML"
  (resource-response "resources/public/index.html"))

(defn int-handler [request]
  "Handler for the internal static resources"
  (-> (format "resources/public/%s/%s"
              (-> request :route-params :resource-type)
              (-> request :route-params :resource-name))
      (resource-response)))

(defn not-found-handler [request]
  "Handler to return 404 responses"
  (not-found "Error 404: Could not find resource"))

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
      calc/process-transaction
      (->> (reset! app-db))))

(defn dump-db [_] @app-db)

(def route-map
  {:save-transaction process-transaction
   :dump-db dump-db})

(defn api-handler
  [request]
  (let [params (transit-decode (:body request))
        handler (some->> params :action (get route-map))]
     (if handler
       {:status 200
        :headers {"Content-Type" "application/transit+json"}
        :body (transit-encode (handler params))}
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
