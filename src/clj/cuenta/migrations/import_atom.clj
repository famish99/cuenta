(ns cuenta.migrations.import-atom
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [cognitect.transit :as transit]
            [clojure.set :as c-set]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]
            [cuenta.db.transactions :as t]))

(defn load-state
  []
  (with-open [fp (-> "data_store.json" io/resource io/input-stream)]
    (->> (transit/reader fp :json)
         transit/read)))

(def app-state (load-state))

(defn migrate-up [config]
  (jdbc/with-db-transaction
    [tx (:conn config)]
    (->> app-state
         :transactions
         (filter :credit-to)
         (map #(t/add-transaction tx %))
         pprint)))
