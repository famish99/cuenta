(ns cuenta.views
  (:require [goog.string :as g-string]
            goog.string.format
            [re-frame.core :as rf]
            [cuenta.add-transaction.views :as a-t]
            [cuenta.components.bootstrap :as bs]
            [cuenta.view-transaction.views :as v-t]
            [cuenta.view-transactions.views :as v-ts]))

;; -- people-matrix components -----------------------------------------------

(defn credit-row
  [owed-cols creditor debts]
  [:tr
   [:td @(rf/subscribe [:get-user-name creditor])]
   (for [d-item owed-cols
         :let [debtor (dissoc d-item :user-name)]]
     ^{:key (g-string/format "%s-owes-%s" creditor debtor)}
     [:td {:style (if (= creditor debtor)
                    {:text-align :right :background :gray}
                    {:text-align :right})}
      (when-let [debt (get debts debtor)]
        (g-string/format "$%.02f" debt))])])

(defn debt-total-col
  [debtor]
  [:td {:style {:text-align :right}}
   [:b (g-string/format "$%.02f" @(rf/subscribe [:total-owed-col debtor]))]])

(defn people-matrix []
  (let [owed-matrix @(rf/subscribe [:owed-table])
        owed-cols @(rf/subscribe [:owed-cols])]
    [bs/panel {:header "Debt Matrix"}
     [bs/table
      [:thead
       [:tr
        [:th "Creditor"]
        (for [{:keys [user-name user-id]} owed-cols]
          ^{:key (g-string/format "debtor-header-%s" user-id)}
          [:th {:style {:text-align :right}} user-name])]]
      [:tbody
       (for [[creditor debts] owed-matrix]
         ^{:key (g-string/format "debt-row-%s" (:user-id creditor))}
         [credit-row owed-cols creditor debts])
       [:tr
        [:td [:b "Total"]]
        (for [debtor owed-cols]
          ^{:key (g-string/format "total-col-%s" (:user-id debtor))}
          [debt-total-col debtor])]]]]))

;; -- home component ---------------------------------------------------------

(defn home []
  [bs/grid {:fluid false}
   [bs/row
    [bs/col {:md 12}
     [people-matrix]]]
   [bs/row
    [bs/col {:md 12}
     [v-ts/recent-transactions]]]])

;; -- router section ---------------------------------------------------------

(def view-map
  {:home home
   :add-transaction a-t/add-transaction
   :view-transaction v-t/view-transaction
   :view-transactions v-ts/view-transactions})

(defn router
  []
  (let [route @(rf/subscribe [:route])]
    [:div
     [bs/navbar {:fluid true}
      [bs/navbar-header
       [bs/navbar-brand
        [bs/button {:bs-style :link
                    :on-click #(rf/dispatch [:reload-home])}
         "Split da Bill"]]]]
     [(get view-map route home)]]))
