(ns cuenta.events
  (:require [ajax.core :as ajax]
            [bidi.bidi :as bidi]
            [day8.re-frame.http-fx]
            [re-frame.core :as rf]
            [cuenta.constants :as const]
            [cuenta.db :as db]
            [cuenta.routes :as rt]))

(def load-home-fx
  [{:method :get
    :uri (bidi/path-for rt/route-map :load-people)
    :timeout const/request-timeout
    :format (ajax/url-request-format)
    :response-format (ajax/transit-response-format)
    :on-success [:update-user-map]
    :on-failure [:dump-error]}
   {:method :get
    :uri (bidi/path-for rt/route-map :load-matrix)
    :timeout const/request-timeout
    :format (ajax/url-request-format)
    :response-format (ajax/transit-response-format)
    :on-success [:update-owed]
    :on-failure [:dump-error]}
   {:method :get
    :uri (bidi/path-for rt/route-map :load-transactions)
    :timeout const/request-timeout
    :format (ajax/url-request-format)
    :response-format (ajax/transit-response-format)
    :on-success [:update-transactions]
    :on-failure [:dump-error]}])

(rf/reg-event-fx
  :initialize-db
  (fn [_ _]
    {:db db/default-db
     :http-xhrio load-home-fx}))

(rf/reg-event-fx
  :load-home
  (fn [{:keys [db]} _]
    {:db (-> (apply dissoc db (keys db/transaction-defaults))
             (dissoc :view-transaction-id :last-id)
             (assoc :route :home))
     :http-xhrio load-home-fx}))

(rf/reg-event-db
  :update-transactions
  (fn [db [_ new-list]]
    (->> db
         :transactions
         (merge new-list)
         (assoc db :transactions))))

(rf/reg-event-db
  :update-route
  (fn [db [_ new-route]]
    (assoc db :route new-route)))

(rf/reg-event-db
  :update-user-map
  (fn [db [_ results]]
    (assoc db :user-map results)))

(defn update-owed
  [world [_ result]]
  {:db (-> world :db (assoc :owed-matrix result))})

(rf/reg-event-fx
  :update-owed
  update-owed)

(rf/reg-event-fx
  :dump-result
  (fn [_ [_ result]]
    (js/console.log result)))

(rf/reg-event-fx
  :dump-error
  (fn [_ [_ error]]
    (js/console.log error)
    (throw (js/Error. error))))
