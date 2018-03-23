(ns cuenta.events
  (:require [ajax.core :as ajax]
            [bidi.bidi :as bidi]
            [cljs.pprint :as pp]
            [day8.re-frame.http-fx]
            [re-frame.core :as rf]
            [cuenta.constants :as const]
            [cuenta.db :as db]
            [cuenta.routes :as rt]
            [cuenta.util :as util]))

(def request-timeout 5000)

(def load-home-fx
  [{:method :get
    :uri (bidi/path-for rt/route-map :load-matrix)
    :timeout request-timeout
    :format (ajax/url-request-format)
    :response-format (ajax/transit-response-format)
    :on-success [:update-owed]
    :on-failure [:dump-result]}
   {:method :get
    :uri (bidi/path-for rt/route-map :load-transactions)
    :timeout request-timeout
    :format (ajax/url-request-format)
    :response-format (ajax/transit-response-format)
    :on-success [:update-transactions]
    :on-failure [:dump-result]}])

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
  :add-transaction
  (fn [db _]
    (-> db
        (assoc :route :add-transaction)
        (merge db/transaction-defaults))))

(defn view-transaction
  [{:keys [db]} [_ t-id]]
  (-> (when-not (get-in db [:transactions t-id :items])
        {:http-xhrio
         {:method :get
          :uri (bidi/path-for rt/route-map
                              :load-transaction
                              :transaction-id t-id)
          :timeout request-timeout
          :format (ajax/url-request-format)
          :response-format (ajax/transit-response-format)
          :on-success [:update-transaction t-id]
          :on-failure [:dump-result]}})
      (merge {:db (assoc db :route :view-transaction
                            :view-transaction-id t-id)})))

(rf/reg-event-fx
  :view-transaction
  view-transaction)

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
        per-page (:per-page db)
        offset (* per-page curr-page)]
    (-> (when (and (<= curr-page (-> db :transactions :page-count))
                   (empty? (nthrest t-list offset)))
          {:http-xhrio
           {:method :post
            :uri (bidi/path-for rt/route-map :load-transactions)
            :timeout request-timeout
            :format (ajax/transit-request-format)
            :params {:last-id (last t-list)
                     :r-limit (- (+ offset per-page) (count t-list))}
            :response-format (ajax/transit-response-format)
            :on-success [:update-transactions]
            :on-failure [:dump-result]}})
        (merge {:db (assoc db :route :view-transactions)}))))

(rf/reg-event-fx
  :transaction-list
  transaction-list)

(rf/reg-event-fx
  :prev-transaction-page
  (fn [{:keys [db] :as world} _]
    (let [curr-page (-> db :transactions :current-page)]
      (if (> curr-page 1)
        (-> world
            (select-keys [:db])
            (update-in [:db :transactions :current-page] dec))
        {:db db}))))

(rf/reg-event-fx
  :next-transaction-page
  (fn [{:keys [db] :as world} _]
    (let [curr-page (-> db :transactions :current-page)
          page-cnt (-> db :transactions :page-count)]
      (if (< curr-page page-cnt)
        (->> (update-in world [:db :transactions :current-page] inc)
             transaction-list)
        {:db db}))))

(rf/reg-event-db
  :update-transaction
  (fn [db [_ t-id result]]
    (assoc-in db [:transactions t-id :items] result)))

(rf/reg-event-db
  :update-creditor
  (fn [db [_ new-value]]
    (assoc db :transaction-owner new-value)))

(defn adjust-item-owners
  [{:keys [people] :as db}]
  (update db :owner-matrix select-keys people))

(defn adjust-items-owned
  [{:keys [owner-matrix items] :as db}]
  (let [item-keys (keys items)]
    (->> (for [[owner items-owned] owner-matrix]
              {owner (select-keys items-owned item-keys)})
         (into {})
         (assoc db :owner-matrix))))

(rf/reg-event-db
  :update-person
  (fn [db [_ pos new-name]]
    (assoc-in db [:people pos] new-name)))

(rf/reg-event-db
  :trim-person
  (fn [db [_ pos]]
    (->> (update (:people db) pos util/trim-string)
         (filter util/not-blank?)
         distinct
         vec
         (assoc db :people)
         adjust-item-owners)))

(rf/reg-event-db
  :update-item
  (fn [db [_ new-value & path]]
    (->> new-value
         (assoc-in (:items db) path)
         (assoc db :items))))

(rf/reg-event-db
  :cast-item
  (fn [db [_ cast-type default-value & path]]
    (->> (update-in (:items db)
                    path
                    (condp = cast-type
                      :int util/format-int
                      :float util/format-float
                      :money util/format-money
                      :string util/trim-string)
                    default-value)
         (filter (fn [[k v]] (util/not-blank? (:item-name v))))
         (into {})
         (assoc db :items)
         adjust-items-owned)))

(rf/reg-event-db
  :update-owner
  (fn [db [_ new-value person-name i-pos]]
    (assoc-in db [:owner-matrix person-name i-pos] new-value)))

(rf/reg-event-db
  :update-tax-rate
  (fn [db [_ new-value]]
    (assoc db :tax-rate new-value)))

(rf/reg-event-db
  :cast-tax-rate
  (fn [db _]
    (update db :tax-rate util/format-float const/default-tax-rate)))

(rf/reg-event-db
  :update-tip-amount
  (fn [db [_ new-value]]
    (assoc db :tip-amount new-value)))

(rf/reg-event-db
  :cast-tip-amount
  (fn [db _]
    (update db :tip-amount util/format-money const/default-tip)))

(rf/reg-event-db
  :update-credit-to
  (fn [db [_ new-value]]
    (assoc db :credit-to new-value)))

(defn update-owed
  [world [_ result]]
  {:db (-> world :db (assoc :owed-matrix result))})

(rf/reg-event-fx
  :update-owed
  update-owed)

(rf/reg-event-db
  :update-vendor-name
  (fn [db [_ new-value]]
    (assoc db :vendor-name new-value)))

(rf/reg-event-fx
  :complete-transaction
  (fn [world disp-v]
    (-> world
        (update-owed disp-v)
        (assoc :dispatch [:load-home]
               :http-xhrio {:method :post
                            :uri (bidi/path-for rt/route-map :load-transactions)
                            :params {}
                            :timeout request-timeout
                            :format (ajax/transit-request-format)
                            :response-format (ajax/transit-response-format)
                            :on-success [:update-transactions]
                            :on-failure [:dump-result]}))))

(rf/reg-event-fx
  :dump-result
  (fn [_ [_ result]]
    (->> result
         (.log js/console))))

(rf/reg-event-fx
  :save-transaction
  (fn [world _]
    {:http-xhrio {:method :post
                  :uri (bidi/path-for rt/route-map :save-transaction)
                  :params (-> world
                              :db
                              (select-keys (keys db/transaction-defaults)))
                  :timeout request-timeout
                  :format (ajax/transit-request-format)
                  :response-format (ajax/transit-response-format)
                  :on-success [:complete-transaction]
                  :on-failure [:dump-result]}}))
