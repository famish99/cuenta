(ns cuenta.add-transaction.views
  (:require [goog.string :as g-string]
            goog.string.format
            [re-frame.core :as rf]
            [cuenta.constants :as const]
            [cuenta.components.bootstrap :as bs]
            [cuenta.components.select :as sel]
            [cuenta.add-transaction.events :as ev]
            [cuenta.add-transaction.subs :as sub]))

;; -- people-panel related components ----------------------------------------

(defn owed-field
  [pos]
  [:td (g-string/format "$%.02f" @(rf/subscribe [::sub/owed pos]))])

(defn person-field
  [pos]
  [:td
   [sel/create
    {:value @(rf/subscribe [::sub/person pos])
     :on-change #(rf/dispatch [::ev/update-person pos (some-> % .-value)])
     :options @(rf/subscribe [::sub/person-suggest pos])
     :ignore-case false}]])

(defn person-entry
  [pos]
  [:tr
   [person-field pos]
   [owed-field pos]])

(defn people-panel
  []
  (let [count-people @(rf/subscribe [::sub/count-new-people])
        claimed-total @(rf/subscribe [::sub/claimed-total])
        total-cost @(rf/subscribe [::sub/total-cost])]
    [bs/panel {:header "People"}
     [bs/table
      [:thead
       [:tr
        [:th "Name"]
        [:th "Owes"]]]
      [:tbody
       (for [pos count-people]
         ^{:key (g-string/format "person-entry-%d" pos)}
         [person-entry pos])
       [:tr
        [:td [:b "Claimed Total"]]
        [:td [:b (when (> (- total-cost claimed-total) 0.01) {:style {:color :red}})
              (g-string/format "$%.02f" claimed-total)]]]]]]))

;; -- order-panel related components -----------------------------------------

;; --- order-panel header components

(defn vendor-name-field
  []
  [:tr
   [:th
    [sel/create
     {:value @(rf/subscribe [::sub/vendor-name-field])
      :on-change #(rf/dispatch [::ev/update-vendor-name (some-> % .-value)])
      :options @(rf/subscribe [::sub/vendor-suggest])
      :ignore-case false}]]])

(defn person-header
  [p-id]
  [:th @(rf/subscribe [::sub/person-field p-id])])

;; --- order-panel item row components

(defn item-checkbox
  [user-value i-pos]
  [:td
   [bs/checkbox
    {:checked @(rf/subscribe [::sub/item-owned? user-value i-pos])
     :on-change #(rf/dispatch [::ev/update-owner (.-target.checked %) user-value i-pos])}]])

(defn item-entry
  [i-pos people]
  (let [item-price @(rf/subscribe [::sub/item-price i-pos])
        item-quantity @(rf/subscribe [::sub/item-quantity i-pos])
        item-taxable @(rf/subscribe [::sub/item-taxable i-pos])]
    [:tr
     [:td.col-xs-4
      [sel/create
       {:value @(rf/subscribe [::sub/item-name i-pos])
        :on-change #(rf/dispatch [::ev/update-item-name (some-> % .-value) i-pos])
        :options @(rf/subscribe [::sub/item-suggest i-pos])
        :ignore-case false}]]
     [:td.col-xs-2
      [bs/form-group
       {:validation-state (:valid-state item-price)}
       [bs/input-group
        [bs/input-group-addon "$"]
        [:input.form-control
         {:type :text
          :value (:value item-price)
          :disabled (:disabled? item-price)
          :on-blur #(rf/dispatch [::ev/cast-item :money const/default-price i-pos :item-price])
          :on-change #(rf/dispatch [::ev/update-item (.-target.value %) i-pos :item-price])}]]]]
     [:td.col-xs-1
      [bs/form-group
       {:validation-state (:valid-state item-quantity)}
       [:input.form-control
        {:type :text
         :value (:value item-quantity)
         :disabled (:disabled? item-quantity)
         :on-blur #(rf/dispatch [::ev/cast-item :int "1" i-pos :item-quantity])
         :on-change #(rf/dispatch [::ev/update-item (.-target.value %) i-pos :item-quantity])}]]]
     [:td
      [bs/checkbox
       {:checked (:value item-taxable)
        :disabled (:disabled? item-taxable)
        :on-change #(rf/dispatch [::ev/update-item (.-target.checked %) i-pos :item-taxable])}]]
     (for [person people]
       ^{:key (g-string/format "row-%d-person-%s" i-pos person)}
       [item-checkbox person i-pos])]))

;; --- order-panel bottom components

(defn tax-rate-field
  []
  (let [tax-rate @(rf/subscribe [::sub/tax-rate-field])
        tax-amount @(rf/subscribe [::sub/tax-amount])]
    [:tr
     [:td [bs/control-label "Tax Rate"]]
     [:td
      [bs/form-group
       {:validation-state (:valid-state tax-rate)}
       [bs/input-group
        [:input.form-control
         {:type :text
          :value (:value tax-rate)
          :on-blur #(rf/dispatch [::ev/cast-tax-rate])
          :on-change #(rf/dispatch [::ev/update-tax-rate (.-target.value %)])}]
        [bs/input-group-addon "%"]]]]
     [:td {:style {:padding-top "14px"}} [:b (g-string/format "= $%.02f" tax-amount)]]]))

(defn tip-amount-field
  []
  (let [tip-amount @(rf/subscribe [::sub/tip-amount-field])]
    [:tr
     [:td [bs/control-label "Tip Amount"]]
     [:td
      [bs/form-group
       {:validation-state (:valid-state tip-amount)}
       [bs/input-group
        [bs/input-group-addon "$"]
        [:input.form-control
         {:type :text
          :value (:value tip-amount)
          :on-blur #(rf/dispatch [::ev/cast-tip-amount])
          :on-change #(rf/dispatch [::ev/update-tip-amount (.-target.value %)])}]]]]]))

(defn total-cost-field
  []
  (let [total-cost @(rf/subscribe [::sub/total-cost])]
    [:tr
     [:td [bs/form-group [bs/control-label "Ticket Total"]]]
     [:td {:style {:text-align :right}} [:b (g-string/format "$%.02f" total-cost)]]]))

(defn credit-to-field
  []
  [:tr
   [:td
    [bs/form-group
     [bs/control-label "Credit to"]]]
   [:td
    [sel/select
     {:clearable false
      :disabled @(rf/subscribe [::sub/credit-to-disabled])
      :value @(rf/subscribe [::sub/credit-to-value])
      :on-change #(rf/dispatch [::ev/update-credit-to (some-> % .-value)])
      :options @(rf/subscribe [::sub/credit-to-suggest])}]]])

;; --- root order-panel component

(defn order-panel
  []
  (let [people @(rf/subscribe [::sub/people])
        count-items @(rf/subscribe [::sub/count-new-items])]
    [bs/panel {:header "Order Info"}
     [bs/table
      [:thead
       [:tr
        [:th
         "Vendor"]]
       [vendor-name-field]
       [:tr
        [:th "Item"]
        [:th "Price"]
        [:th "Quantity"]
        [:th "Taxable"]
        (for [p-id people]
          ^{:key (g-string/format "items-person-%s" p-id)}
          [person-header p-id])]]
      [:tbody
       (for [i-pos count-items]
         ^{:key (g-string/format "item-row-%d" i-pos)}
         [item-entry i-pos people])
       [tax-rate-field]
       [tip-amount-field]
       [total-cost-field]
       [credit-to-field]
       [:tr
        [:td {:col-span 2}
         [bs/button {:bs-style :primary
                     :on-click #(rf/dispatch [::ev/save-transaction])}
          [bs/glyphicon {:glyph :floppy-disk}] " Save"]]]]]]))

;; -- root add-transaction component -----------------------------------------

(defn add-transaction []
  [bs/grid {:fluid false}
   [bs/row
    [bs/col {:md 6}
     [people-panel]]]
   [bs/row
    [bs/col {:md 12}
     [order-panel]]]])

