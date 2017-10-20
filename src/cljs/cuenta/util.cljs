(ns cuenta.util
  (:require [clojure.string :refer [blank?]]
            [goog.string :as g-string]))

(def not-blank? (complement blank?))

(defn format-int
  [input-value default-value]
  (let [value (js/parseInt input-value)]
    (if (js/isNaN value)
      default-value
      (g-string/format "%d" value))))

(defn format-money
  [input-value default-value]
  (let [value (js/parseFloat input-value)]
    (if (js/isNaN value)
      default-value
      (g-string/format "%.2f" value))))

(defn format-float
  [input-value default-value]
  (let [value (js/parseFloat input-value)]
    (if (js/isNaN value)
      default-value
      (g-string/format "%f" value))))
