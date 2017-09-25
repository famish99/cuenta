(ns cuenta.util
  (:require [clojure.string :refer [blank?]]))

(def not-blank? (complement blank?))
