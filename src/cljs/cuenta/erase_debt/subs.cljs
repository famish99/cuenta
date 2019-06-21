(ns cuenta.erase-debt.subs
  (:require [re-frame.core :as rf]
            [cuenta.subs :refer [compare-with-key]]
            [cuenta.erase-debt.events :as ev]))


(rf/reg-sub
  ::selected-cells
  (fn [db _]
    (::ev/selected-cells db)))

(rf/reg-sub
  ::owed-cell
  :<- [:owed-matrix]
  :<- [::selected-cells]
  (fn [[owed-matrix selected-cells] [_ creditor debtor]]
    (let [attr-base {:style {:text-align :right}}]
      (cond
        (= creditor debtor)
        (assoc-in attr-base [:style :background-color] :gray)

        (get-in owed-matrix [creditor debtor])
        (-> attr-base
            (assoc :on-click #(rf/dispatch [::ev/select-cell creditor debtor]))
            (assoc-in [:style :cursor] :pointer)
            (cond-> (get selected-cells [creditor debtor])
                    (assoc-in [:style :background-color] :lightgreen)))

        :else attr-base))))

(rf/reg-sub
  ::debt-list
  :<- [:owed-matrix]
  :<- [:user-map]
  :<- [::selected-cells]
  (fn [[owed-matrix user-map selected-cells] _]
    (for [[c-id d-id] selected-cells
          :let [creditor (->> c-id :user-id (get user-map))
                debtor (->> d-id :user-id (get user-map))]]
      [creditor debtor (get-in owed-matrix [c-id d-id])])))
