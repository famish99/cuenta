(ns cuenta.calc)

(def max-value
  #?(:clj Double/MAX_VALUE
     :cljs (aget js/Number "MAX_VALUE")))

(defn cast-float
  [input-value & {:keys [default] :or {default 0}}]
  #?(:clj (if (string? input-value) (Double/parseDouble input-value) default)
     :cljs (let [amount (js/parseFloat input-value)] (if (js/isNaN amount) default amount))))

(defn cast-int
  [input-value & {:keys [default] :or {default 0}}]
  #?(:clj (if (string? input-value) (Integer/parseInt input-value) default)))

(defn item-calc
  [{:keys [item-price item-quantity item-taxable]
    :or {item-quantity 1 item-taxable true}}
   tax-rate]
  (* item-price
     (if (false? item-taxable) 1 (+ 1 (/ tax-rate 100)))
     (int (or item-quantity 1))))

(defn calc-item-cost
  [[items owners tax-rate] & _]
  (for [[item-key item-element] items]
    (as-> owners r
      (vals r)
      (map #(if (get % item-key) 1 0) r)
      (reduce + r)
      [item-key
       [r (/ (item-calc item-element tax-rate)
             (max r 1))]]))) ; prevent divide by zero

(defn calc-owed
  [[costs owners tip-amount total] person-name]
  (as-> costs r
    (for [[k [_ cost]] r
          :when (-> owners (get person-name) (get k))]
      cost)
    (reduce + r)
    (+ r (* tip-amount (/ r
                          (max 1 ; prevent divide by zero
                               (- total tip-amount)))))))

(defn total-cost
  [def-owner [costs tip-amount]]
  (->> (for [[_ [n cost]] costs] (* (max def-owner n) cost))
       (apply (partial + tip-amount))))

(defn trace-debt
  [owed-matrix trace node p-value]
  (let [f-node (filter (fn [[_ v]] (> p-value v)) node)]
    (if (> (count f-node) 0)
      (->>
        (for [[n-key n-value] f-node]
          (trace-debt owed-matrix
                      (conj trace n-key)
                      (if (some #{n-key} trace) {} (get owed-matrix n-key))
                      n-value))
        (into {}))
      {trace [p-value (* (count trace) p-value)]})))

(defn find-optimal-trace
  [owed-matrix]
  (->>
    (for [[key node] owed-matrix]
      (trace-debt owed-matrix [key] node max-value))
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
