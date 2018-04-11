(ns cuenta.db
  (:require [cuenta.constants :as const]))

(def transaction-defaults
  {:people []
   :items {}
   :tax-rate const/default-tax-rate
   :tip-amount const/default-tip
   :owner-matrix {}
   :vendor-name nil
   :vendor-map {}
   :item-map {}
   :credit-to nil})

(def default-db
  {:route :home
   :owed-matrix {}
   :cuenta.view-transactions.subs/per-page 10
   :user-map {}
   :transactions {:current-page 1}})
