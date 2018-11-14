(ns cuenta.core
  (:require [bidi.bidi :as bidi]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [cuenta.codecs :as cd]
            [cuenta.db :as db]
            [cuenta.handlers :as h]
            [cuenta.routes :as routes]
            [cuenta.ws :as ws]))

(defn dump-n-pass [input] (log/info input) input)

(defn decode-body
  [request transit-type]
  (-> request
      :body
      (cd/transit-decode :msg-type transit-type)))

(defn find-transit-type
  [request]
  (some-> request
          (get-in [:headers "content-type"])
          (->> (re-find #"^application/transit\+(json|msgpack)"))
          second
          keyword))

(defn gen-api-handler
  [handler]
  (fn [request]
    {:status 200
     :headers {"Content-Type" "application/transit+json"
               "Cache-Control" "no-cache, no-store, must-revalidate"
               "Pragma" "no-cache"
               "Expires" 0}
     :body (jdbc/with-db-transaction
             [tx db/db-spec]
             (db/clear-transaction-cache!)
             (let [transit-type (find-transit-type request)]
               (-> (when transit-type (decode-body request transit-type))
                   (merge (:route-params request))
                   (->> (handler tx))
                   (cd/transit-encode :msg-type (or transit-type :json)))))}))

(def socket-map
  (->> (for [[k [handler h-type]] h/handler-map
             :when (= h-type :api)]
         {k handler})
       (into {})))

(def backend-map
  (->> (for [[k [handler h-type]] h/handler-map]
         {k (case h-type
              :raw handler
              :api (gen-api-handler handler))})
       (into {:web-socket (ws/gen-ws-handler socket-map)})))

(defn static-handler
  [request]
  (let [{:keys [handler route-params]}
        (bidi/match-route routes/route-map (:uri request))]
    (log/info (:uri request))
    (assert handler :static-resource)
    (h/int-handler (assoc request :route-params route-params))))

(defn backend-handler
  [request]
  (log/info (:uri request))
  (if-let [{:keys [handler route-params]}
           (bidi/match-route routes/route-map (:uri request))]
    (if-let [handler-f (handler backend-map)]
      (handler-f (assoc request :route-params route-params))
      ; else
      (h/index-handler request))
    (h/not-found-handler request)))
