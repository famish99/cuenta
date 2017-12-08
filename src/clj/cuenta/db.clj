(ns cuenta.db
  (:require [hugsql.core :as hug]))

(def db-spec (get (System/getenv) "DATABASE_URL"))

(def m-config {:store :database
               :migration-dir "migrations/"
               :db db-spec})
