(ns cuenta.view-transaction.views
  (:require [goog.string :as g-string]
            goog.string.format
            [re-frame.core :as rf]
            [cuenta.components.bootstrap :as bs]))

(defn view-transaction []
  (let [t-id @(rf/subscribe [:transaction-id])
        t-details @(rf/subscribe [:t-details t-id])
        items (:items t-details)]
    [bs/grid {:fluid false}
     [:h5
      [:a.make-link {:on-click #(rf/dispatch [:transaction-list])
                     :tab-index 0}
       bs/back-arrow " Transaction List"]]
     [bs/panel {:header (:vendor-name t-details)}
      [bs/table
       [:thead
        [:tr
         [:th "Item"]
         [:th "Price"]
         [:th "Quantity"]
         [:th "Taxable"]
         [:th "Owners"]]]
       [:tbody
        (if (empty? items)
          [:tr [:td.center-text {:col-span 5} "Loading..."]]
          (for [[i-id {:keys [item-name item-price item-quantity item-taxable owners]}]
                (:items t-details)]
            ^{:key (g-string/format "item-row-%d" i-id)}
            [:tr
             [:td item-name]
             [:td (g-string/format "$%.2f" item-price)]
             [:td item-quantity]
             [:td [bs/glyphicon {:glyph (if item-taxable :ok :remove)}]]
             [:td (->> owners
                       (map :given-name)
                       (interpose ", ")
                       (apply str))]]))
        [:tr
         [:td [:b "Tip Amount"]]
         [:td (g-string/format "$%.2f" (:tip-amount t-details))]]
        [:tr
         [:td [:b "Total"]]
         [:td (g-string/format "$%.2f" (:total-cost t-details))]]
        [:tr
         [:td [:b "Credit to"]]
         [:td (:given-name t-details)]]]]]]))
