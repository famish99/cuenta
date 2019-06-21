(ns cuenta.erase-debt.views
  (:require [goog.string :as g-string]
            goog.string.format
            [reagent.core :as r]
            [re-frame.core :as rf]
            [cuenta.components.bootstrap :as bs]
            [cuenta.erase-debt.events :as ev]
            [cuenta.erase-debt.subs :as sub]))

(defn credit-row
  [owed-cols creditor debts]
  [:tr
   [:td @(rf/subscribe [:get-user-name creditor])]
   (doall
     (for [d-item owed-cols
           :let [debtor (dissoc d-item :user-name)]]
       ^{:key (g-string/format "%s-owes-%s" creditor debtor)}
       [:td @(rf/subscribe [::sub/owed-cell creditor debtor])
        (when-let [debt (get debts debtor)]
          (g-string/format "$%.02f" debt))]))])

(defn erase-debt []
  (let [confirm-erase (r/atom false)]
    (fn []
      (let [owed-matrix @(rf/subscribe [:owed-table])
            owed-cols @(rf/subscribe [:owed-cols])
            debt-list @(rf/subscribe [::sub/debt-list])]
        [bs/grid
         [bs/row
          [bs/col {:md 12}
           [bs/panel {:header "Choose debt cells to clear"}
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
               [:td
                [bs/button
                 {:bs-style :primary
                  :on-click #(reset! confirm-erase true)}
                 [:span.glyphicon.glyphicon-floppy-disk] bs/nbsp "Clear Debt"]]]]]]]]
         [bs/modal {:show @confirm-erase
                    :on-hide #(reset! confirm-erase false)}
          [bs/modal-header {:close-button true}
           [bs/modal-title "Confirm Clearing Debts"]]
          [bs/modal-body
           [bs/table
            [:thead
             [:tr [:th "Confirm payment"]]]
            [:tbody
             (for [[creditor debtor amount] debt-list]
               ^{:key (g-string/format "conf-%s-%s" debtor creditor)}
               [:tr
                [:td (g-string/format "%s paid %s: " debtor creditor)
                 [:b (g-string/format "$%.02f" amount)]]])]]]
          [bs/modal-footer
           [bs/button {:on-click #(reset! confirm-erase false)} "Cancel"]
           [bs/button
            {:bs-style :primary
             :on-click #(rf/dispatch [::ev/save-erase])}
            [:span.glyphicon.glyphicon-floppy-disk] bs/nbsp "Save"]]]]))))

