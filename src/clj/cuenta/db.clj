(ns cuenta.db
  (:require [hugsql.core :as hug]))

(def db-spec (get (System/getenv) "DATABASE_URL"))

(def m-config {:store :database
               :migration-dir "migrations/"
               :db db-spec})

(def mem (atom {}))

(defn clear-transaction-cache! [] (reset! mem {}))

(defn memoize-transaction
  "Returns a memoized version of a referentially transparent function with the
  intention of reducing database IO for previously made queries within a
  transaction."
  [f]
  (fn [& args]
    (if-let [e (some-> @mem (get f) (find args))]
      (val e)
      (let [ret (apply f args)]
        (swap! mem assoc-in [f args] ret)
        ret))))
