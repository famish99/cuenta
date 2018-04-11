(ns cuenta.view-transactions.views
  (:require [goog.string :as g-string]
            goog.string.format
            [re-frame.core :as rf]
            [cuenta.components.bootstrap :as bs]
            [cuenta.view-transactions.events :as ev]
            [cuenta.view-transactions.subs :as sub]))

(defn transaction-entry
  [t-id]
  [:tr
   [:td [:a.make-link
         {:on-click #(rf/dispatch [::ev/view-transaction t-id])
          :tab-index 0}
         @(rf/subscribe [::sub/t-item-vendor t-id])]]
   [:td @(rf/subscribe [::sub/t-item-purchaser t-id])]
   [:td @(rf/subscribe [::sub/t-item-cost t-id])]
   [:td @(rf/subscribe [::sub/t-item-date t-id])]])

(defn view-transactions []
  (let [t-list @(rf/subscribe [::sub/transaction-list])
        curr-page @(rf/subscribe [::sub/curr-t-page])
        page-count @(rf/subscribe [::sub/tot-t-page])]
    [bs/grid {:fluid false}
     [:div.panel.panel-default
      [:div.panel-heading "Transactions"]
      [:div.panel-body
       [bs/table
        [:thead
         [:tr
          [:th "Vendor"]
          [:th "Purchaser"]
          [:th "Cost"]
          [:th "Date Added"]]]
        [:tbody
         (if (empty? t-list)
           [:tr [:td.center-text {:col-span 4} "Loading..."]]
           (for [t-id t-list]
             ^{:key (g-string/format "transaction-row-%d" t-id)}
             [transaction-entry t-id]))]]
       [:ul.pager
        [:li.previous
         (when (= curr-page 1) {:style {:visibility :hidden}})
         [:a.make-link
          {:tab-index 0
           :on-click #(rf/dispatch [::ev/prev-transaction-page])}
          bs/left-arrow " Prev"]]
        [:li
         [:span {:style {:border :none}}
          (g-string/format "%d of %d" curr-page page-count)]]
        [:li.next
         (when (= curr-page page-count) {:style {:visibility :hidden}})
         [:a.make-link
          {:tab-index 0
           :on-click #(rf/dispatch [::ev/next-transaction-page])}
          "Next " bs/right-arrow]]]]]]))

;; -- recent-transaction component -------------------------------------------

(defn recent-transactions []
  (let [t-list @(rf/subscribe [::sub/recent-transactions])]
    [:div.panel.panel-default
     [:div.panel-heading
      [:a.make-link {:on-click #(rf/dispatch [::ev/transaction-list])
                     :tab-index 0}
       "Transactions"]]
     [:div.panel-body
      [bs/table
       [:thead
        [:tr
         [:th "Vendor"]
         [:th "Purchaser"]
         [:th "Cost"]
         [:th "Date Added"]]]
       [:tbody
        (for [t-id t-list]
          ^{:key (g-string/format "transaction-row-%d" t-id)}
          [transaction-entry t-id])
        [:tr
         [:td
          [bs/button
           {:bs-style :primary
            :on-click #(rf/dispatch [:cuenta.add-transaction.events/add-transaction])}
           [bs/glyphicon {:glyph :plus}] " Add Transaction"]]]]]]]))
