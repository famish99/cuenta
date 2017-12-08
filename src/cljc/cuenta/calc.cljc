(ns cuenta.calc
  (:require [clojure.pprint :refer [pprint]]))

(def max-value
  #?(:clj Double/MAX_VALUE
     :cljs (aget js/Number "MAX_VALUE")))

(defn item-cost-per-person
  [owners tax-rate]
  (fn [[item-key {:keys [item-price item-quantity item-taxable]
                  :or {item-quantity 1 item-taxable true}}]]
    [item-key (* item-price
                 (if (false? item-taxable) 1 (+ 1 (/ tax-rate 100)))
                 (/ (int (or item-quantity 1))
                    (->> owners
                         vals
                         (map #(if (get % item-key) 1 0))
                         (reduce +)
                         (max 1))))])) ; prevent divide by zero

(defn calc-item-cost
  [[items owners tax-rate] & _]
  (map (item-cost-per-person owners tax-rate) items))

(defn calc-owed
  [[costs owners] person-name]
  (->> costs
       (filter (fn [[k _]] (get (get owners person-name) k)))
       (map second)
       (reduce +)))

(defn trace-debt
  [owed-matrix trace key node p-value]
  (let [f-node (filter (fn [[_ v]] (> p-value v)) node)]
    (if (> (count f-node) 0)
      (->>
        (for [[n-key n-value :as n-pair] f-node]
          (trace-debt owed-matrix
                      (conj trace n-key)
                      n-key
                      (if (some #{n-key} trace) {} (get owed-matrix n-key))
                      n-value))
        (into {}))
      {trace [p-value (* (count trace) p-value)]})))

(defn find-optimal-trace
  [owed-matrix]
  (->>
    (for [[key node] owed-matrix]
      (trace-debt owed-matrix [key] key node max-value))
    (into {})
    (filter (fn [[t _]] (> (count t) 2)))
    (reduce (fn [[_ [_ o-t] :as o-p] [_ [_ n-t] :as n-p]]
              (if (< o-t n-t) n-p o-p))
            [[] [0 0]])))

(defn adjust-tail
  [owed-matrix [f-p :as trace] amount]
  (let [l-p (last trace)]
    (if (= f-p l-p)
      owed-matrix
      (update-in owed-matrix [f-p l-p] #(+ (or %1 0) %2) amount))))

(defn adjust-debt
  ([owed-matrix [trace [amount _]]]
   (-> owed-matrix
       (adjust-debt trace amount)
       (adjust-tail trace amount)))
  ([owed-matrix [f-p s-p :as trace] amount]
   (if (= (count trace) 2)
     (update owed-matrix f-p dissoc s-p)
     (recur (update-in owed-matrix [f-p s-p] - amount) (rest trace) amount))))

(defn reduce-debts
  [owed-matrix]
  (let [[trace :as trace-data] (find-optimal-trace owed-matrix)]
    (if (= (count trace) 0)
      owed-matrix
      (recur (adjust-debt owed-matrix trace-data)))))
