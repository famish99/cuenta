(ns cuenta.view-transaction.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
  ::transaction-id
  (fn [db _]
    (:view-transaction-id db)))

(rf/reg-sub
  ::t-details
  (fn [db [_ t-id]]
    (get-in db [:transactions t-id])))
