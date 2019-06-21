(ns cuenta.view-transactions.events
  (:require [re-frame.core :as rf]))

;; -- transaction detail section ---------------------------------------------

(rf/reg-event-db
  ::update-transaction
  (fn [db [_ t-id result]]
    (assoc-in db [:transactions t-id :items] result)))

(defn view-transaction
  [{:keys [db]} [_ t-id]]
  (-> (when-not (get-in db [:transactions t-id :items])
        {:api
         {:action       :load-transaction
          :route-params {:transaction-id (int t-id)}
          :on-success   [::update-transaction t-id]
          :on-failure   [:dump-error]}})
      (merge {:db (assoc db :route :view-transaction
                            :view-transaction-id t-id)})))

(rf/reg-event-fx
  ::view-transaction
  view-transaction)

;; -- transaction list section -----------------------------------------------

(defn get-transaction-list-keys
  [db]
  (-> db
      :transactions
      (dissoc :page-count :current-page)
      keys
      (->> (sort >))))

(defn transaction-list
  [{:keys [db]} & _]
  (let [t-list (get-transaction-list-keys db)
        curr-page (-> db :transactions :current-page)
        per-page (:cuenta.view-transactions.subs/per-page db)
        offset (* per-page curr-page)]
    (-> (when (and (<= curr-page (-> db :transactions :page-count))
                   (empty? (nthrest t-list offset)))
          {:api
           {:action :load-transactions
            :params {:last-id (last t-list)
                     :r-limit (- (+ offset per-page) (count t-list))}
            :on-success [:update-transactions]
            :on-failure [:dump-error]}})
        (merge {:db (assoc db :route :view-transactions)}))))

(rf/reg-event-fx
  ::transaction-list
  transaction-list)

(rf/reg-event-fx
  ::prev-transaction-page
  (fn [{:keys [db] :as world} _]
    (let [curr-page (-> db :transactions :current-page)]
      (if (> curr-page 1)
        (-> world
            (select-keys [:db])
            (update-in [:db :transactions :current-page] dec))
        {:db db}))))

(rf/reg-event-fx
  ::next-transaction-page
  (fn [{:keys [db] :as world} _]
    (let [curr-page (-> db :transactions :current-page)
          page-cnt (-> db :transactions :page-count)]
      (if (< curr-page page-cnt)
        (->> (update-in world [:db :transactions :current-page] inc)
             transaction-list)
        {:db db}))))
