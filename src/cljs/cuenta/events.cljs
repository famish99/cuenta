(ns cuenta.events
  (:require [re-frame.core :as rf]
            [cuenta.ws :as ws]
            [cuenta.db :as db]))


(def load-home-fx
  [{:action :load-people
    :on-success [:update-user-map]
    :on-failure [:dump-error]}
   {:action :load-matrix
    :on-success [:update-owed]
    :on-failure [:dump-error]}
   {:action :load-transactions
    :on-success [:update-transactions]
    :on-failure [:dump-error]}])

(rf/reg-event-fx
  :initialize-db
  (fn [_ _]
    {:db db/default-db
     :open-ws {}
     :api load-home-fx}))

(rf/reg-event-fx
  :load-home
  [ws/ws-active]
  (fn [{:keys [db ws-active]} _]
    (cond-> {:db (-> (apply dissoc db (keys db/transaction-defaults))
                     (dissoc :view-transaction-id
                             :last-id
                             :cuenta.erase-debt.events/selected-cells)
                     (assoc :route :home))}
            (not ws-active) (assoc :api load-home-fx))))

(rf/reg-event-fx
  :reload-home
  (fn [{:keys [db]} _]
    {:db (-> (apply dissoc db (keys db/transaction-defaults))
             (dissoc :view-transaction-id :last-id)
             (assoc :route :home))
     :api load-home-fx}))

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

(rf/reg-event-fx :update-owed update-owed)

(rf/reg-event-fx
  :dump-result
  (fn [_ [_ result]]
    (js/console.log result)))

(rf/reg-event-fx
  :dump-error
  (fn [_ [_ error]]
    (js/console.log error)
    (throw (js/Error. error))))
