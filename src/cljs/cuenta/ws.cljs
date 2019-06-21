(ns cuenta.ws
  (:require [ajax.core :as ajax]
            [bidi.bidi :as b]
            [goog.object]
            [re-frame.core :as rf]
            [cuenta.codecs :as cd]
            [cuenta.routes :as cr]
            [cuenta.constants :as const]))

(defonce socket (atom nil))

(defn msg-rx-handler
  [event]
  (-> event
      .-data
      cd/transit-decode
      rf/dispatch))

(defn open-ws
  [_]
  (when-not @socket
    (let [host (-> js/document
                   (goog.object/get "location")
                   .-host)
          conn (-> (b/path-for cr/route-map :web-socket)
                   (->> (str "ws://" host))
                   (js/WebSocket. "transit+json"))]
      (set! (.-onopen conn) (fn [_] (reset! socket conn)))
      (set! (.-onmessage conn) msg-rx-handler)
      (set! (.-onclose conn) (fn [_] (reset! socket nil)))))) ; should also dispatch event

(def ws-active
  (rf/->interceptor
    :id :ws-active
    :before
    (fn [context]
      (assoc-in context [:coeffects :ws-active] (boolean @socket)))))

(rf/reg-fx :open-ws open-ws)

(defn send-request
  [{:keys [action params route-params on-success on-error] :as fx-params}]
  (if @socket
    (try
      (->> fx-params cd/transit-encode (.send @socket))
      (catch js/Error err
        (-> on-error (conj err) rf/dispatch)))
    (ajax/ajax-request
      (-> (if params
            {:method :post
             :params params
             :format (ajax/transit-request-format)}
            {:method :get
             :format (ajax/url-request-format)})
          (merge {:uri (b/unmatch-pair cr/route-map
                                       {:handler action
                                        :params route-params})
                  :timeout const/request-timeout
                  :response-format (ajax/transit-response-format)
                  :handler (fn [[success? response]]
                             (if success?
                               (->> response
                                    (conj on-success)
                                    rf/dispatch)
                               (->> response
                                    (conj on-error)
                                    rf/dispatch)))})))))

(rf/reg-fx
  :api
  (fn [req]
    (doseq [request (if (sequential? req) req [req])]
      (send-request request))))
