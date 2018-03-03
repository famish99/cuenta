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

(rf/reg-event-fx
  :initialize-db
  (fn [_ _]
    {:db db/default-db
     :dispatch [:init-home]}))

(rf/reg-event-db
  :load-home
  (fn [db _]
    (-> (apply dissoc db (keys db/transaction-defaults))
        (assoc :route :home))))

(rf/reg-event-fx
  :init-home
  (fn [_ _]
    {:http-xhrio [{:method :get
                   :uri (bidi/path-for rt/route-map :load-matrix)
                   :timeout 5000
                   :format (ajax/url-request-format)
                   :response-format (ajax/transit-response-format)
                   :on-success [:update-owed]
                   :on-failure [:dump-result]}
                  {:method :post
                   :uri (bidi/path-for rt/route-map :load-transactions)
                   :params {}
                   :timeout 5000
                   :format (ajax/transit-request-format)
                   :response-format (ajax/transit-response-format)
                   :on-success [:update-transactions]
                   :on-failure [:dump-result]}]}))

(rf/reg-event-db
  :update-transactions
  (fn [db [_ new-list]]
    (assoc db :transactions new-list)))

(rf/reg-event-db
  :update-route
  (fn [db [_ new-route]]
    (assoc db :route new-route)))

(rf/reg-event-db
  :add-transaction
  (fn [db _]
    (-> db
        (assoc :route :transaction)
        (merge db/transaction-defaults))))

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
                            :timeout 5000
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
                  :timeout 5000
                  :format (ajax/transit-request-format)
                  :response-format (ajax/transit-response-format)
                  :on-success [:complete-transaction]
                  :on-failure [:dump-result]}}))
