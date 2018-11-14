(ns cuenta.ws
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as str]
            [immutant.web.async :as ws]
            [cuenta.db :as db]
            [cuenta.codecs :as cd])
  (:import (io.undertow.util HttpString)
           (java.io ByteArrayInputStream)))

(def ws-proto-key (HttpString. "Sec-WebSocket-Protocol"))

(def ws-proto-map
  {:transit+json    {:decode-f #(-> %
                                    .getBytes
                                    ByteArrayInputStream.
                                    (cd/transit-decode :msg-type :json))
                     :encode-f #(-> %
                                    (cd/transit-encode :msg-type :json)
                                    slurp)}
   :transit+msgpack {:encode-f #(cd/transit-encode % :msg-type :msgpack)
                     :decode-f #(cd/transit-decode % :msg-type :msgpack)}})

(defonce conn-pool (atom {}))

(defn gen-msg-handler
  [{:keys [decode-f encode-f]} handler-map]
  (fn [ch message]
    (when-let [{:keys [on-success on-error params route-params]
                :as req}
               (some-> message decode-f)]
      (try
        (jdbc/with-db-connection
          [tx db/db-spec]
          (db/clear-transaction-cache!)
          (->> ((-> req :action handler-map) tx (merge params route-params))
               (conj on-success)
               encode-f
               (ws/send! ch)))
        (catch Exception e
          (->> e
               (conj on-error)
               encode-f
               (ws/send! ch)))))))

(defn gen-ws-handler
  [handler-map]
  (fn [request]
    (let [protocol (-> request
                       :server-exchange
                       .getRequestHeaders
                       (.get ws-proto-key 0)
                       (str/split #", " 2)
                       first)]
      (-> request
          :server-exchange
          .getResponseHeaders
          (.put ws-proto-key protocol))
      (ws/as-channel request
                     :on-open (fn [ch]
                                (->> protocol
                                     keyword
                                     (swap! conn-pool assoc ch)))
                     :on-message (-> protocol
                                     keyword
                                     ws-proto-map
                                     (gen-msg-handler handler-map))
                     :on-error (fn [_ err] (throw err))
                     :on-close (fn [ch _] (swap! conn-pool dissoc ch))))))

(defn broadcast-events
  [& events]
  (doseq [[ch proto] @conn-pool
          event events
          :let [{:keys [encode-f]} (get ws-proto-map proto)]]
    (->> event
         encode-f
         (ws/send! ch))))
