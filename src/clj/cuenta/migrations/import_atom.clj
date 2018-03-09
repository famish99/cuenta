(ns cuenta.migrations.import-atom
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [cognitect.transit :as transit]
            [clojure.pprint :refer [pprint]]
            [cuenta.db :as db]
            [cuenta.db.transactions :as t]))

(defn load-state
  [path]
  (with-open [fp (-> path io/resource io/input-stream)]
    (->> (transit/reader fp :json)
         transit/read)))

(def app-state (load-state "data_store.json"))

(def initial-debts (load-state "initial_debts.json"))

(defn migrate-up [config]
  (jdbc/with-db-transaction
    [tx (:conn config)]
    (db/clear-transaction-cache!)
    (->> initial-debts
         (t/add-debts tx))
    (->> app-state
         :transactions
         (filter :credit-to)
         (map #(t/process-transaction tx %))
         doall)))
