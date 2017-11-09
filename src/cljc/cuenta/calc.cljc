(ns cuenta.calc)

(defn item-cost-per-person
  [owners tax-rate]
  (fn [[item-key {:keys [item-price item-quantity item-taxable]
                  :or {item-quantity 1 item-taxable true}}]]
    [item-key (* item-price
                 (if item-taxable (+ 1 (/ tax-rate 100)) 1)
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

(defn reduce-debts-simple
  [owed-matrix]
  (for [[creditor credits] owed-matrix
        [debtor debt] credits]
    (if-let [rev-credit (get-in owed-matrix [debtor creditor])]
      (if (> debt rev-credit)
        {creditor {debtor (- debt rev-credit)}}
        :erase)
      {creditor {debtor debt}})))

(defn trace-debt
  [owed-matrix trace key node p-value]
  (let [f-node (filter (fn [[_ v]] (> p-value v)) node)]
    (if (> (count f-node) 0)
      (->>
        (for [[n-key n-value :as n-pair] f-node]
          (let [n-node (get owed-matrix n-key)]
            (if (some #{n-key} trace)
              n-pair
              {n-pair (trace-debt owed-matrix
                                  (conj trace n-key)
                                  n-key
                                  n-node
                                  n-value)})))
        (into {}))
      [trace (* (count trace) p-value)])))

(defn reduce-debts
  [owed-matrix]
  (->>
    (for [[key node] owed-matrix]
      [key (trace-debt owed-matrix [key] key node 100)])
    (into {})))

(defn process-transaction
  [db]
  (let [curr-t (-> db :transactions last)
        credit-to (or (:credit-to curr-t) (-> curr-t (get :people) first))
        owners (:owner-matrix curr-t)
        item-costs (calc-item-cost [(:items curr-t) owners (:tax-rate curr-t)])]
    (->> curr-t
         :people
         (remove #{credit-to})
         (map #(vector % (calc-owed [item-costs owners] %)))
         (into {})
         (update (:owed-matrix db) credit-to (partial merge-with +))
         reduce-debts
         (remove #{:erase})
         (apply merge-with conj))))
