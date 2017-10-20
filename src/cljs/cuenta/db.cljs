(ns cuenta.db
  (:require [cuenta.constants :as const]))

(def transaction-defaults
  {:people []
   :items {}
   :tax-rate const/default-tax-rate
   :owner-matrix {}})

(def default-db
  {:route :home :owed-matrix {}})
