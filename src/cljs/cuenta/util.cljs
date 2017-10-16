(ns cuenta.util
  (:require [clojure.string :as c-string]
            [goog.string :as g-string]
            goog.string.format))

(def not-blank? (complement c-string/blank?))

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

(defn trim-string
  [input-value]
  (when input-value
    (c-string/trim input-value)))
