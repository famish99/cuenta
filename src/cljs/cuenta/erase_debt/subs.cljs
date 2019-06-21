(ns cuenta.erase-debt.subs
  (:require [goog.string :as g-string]
            goog.string.format
            [re-frame.core :as rf]
            [cuenta.components.bootstrap :as bs]
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

