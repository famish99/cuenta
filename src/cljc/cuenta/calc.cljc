(ns cuenta.calc)

(defn item-cost-per-person
  [owners tax-rate]
  (fn [[item-key {:keys [item-price item-quantity item-taxable]
                  :or {item-quantity 1 item-taxable true}}]]
    [item-key (* item-price
                 (if item-taxable (+ 1 (/ tax-rate 100)) 1)
                 (/ (int item-quantity)
                    (max (reduce + (map #(if (get % item-key) 1 0) (vals owners)))
                         1)))])) ; prevent divide by zero

(defn calc-item-cost
  [[items owners tax-rate] & _]
  (map (item-cost-per-person owners tax-rate) items))

(defn calc-owed
  [[costs owners] person-name]
  (->> costs
       (filter (fn [[k _]] (get (get owners person-name) k)))
       (map second)
       (reduce +)))

(defn process-transaction
  [db]
  (let [credit-to (or (:credit-to db) (-> db (get :people) first))
        owners (:owner-matrix db)
        item-costs (calc-item-cost [(:items db) owners (:tax-rate db)])]
    (-> db
        :people
        (->> (remove #{credit-to})
             (map #(vector % (calc-owed [item-costs owners] %)))
             (into {})
             (update-in db [:owed-matrix credit-to] (partial merge-with +))))))
