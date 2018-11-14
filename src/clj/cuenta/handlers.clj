(ns cuenta.handlers
  (:require [cuenta.db.transactions :as t]
            [cuenta.calc :as calc]
            [ring.util.response :as ring-r]
            [ring.util.mime-type :as mime]))

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
           (update :item-quantity calc/cast-int :default 1))})

(defn loopback-handler
  [request]
  {:status 200
   :headers {"Content-Type" "application/transit+json; charset=utf-8"}
   :body (:body request)})

(defn process-transaction
  [tx params]
  (-> params
      (update :items #(->> % (map adjust-items) (into {})))
      (update :tax-rate calc/cast-float)
      (update :tip-amount calc/cast-float)
      (update :credit-to #(or % (-> params :people first)))
      (->> (t/process-transaction tx))))

(def handler-map
  {:index             [index-handler :raw]
   :static-resource   [int-handler :raw]
   :load-transaction  [t/find-transaction :api]
   :load-transactions [t/find-transactions :api]
   :save-transaction  [process-transaction :api]
   :load-matrix       [t/find-debt :api]
   :load-people       [t/find-users :api]
   :load-items        [t/find-items :api]
   :load-vendors      [t/find-vendors :api]
   :loopback          [loopback-handler :raw]})
