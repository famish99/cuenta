(ns cuenta.views
  (:require [goog.string :as g-string]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as re-com]
            [cuenta.components.bootstrap :as bs]))

(defn person-entry
  [pos]
  (let [name-value @(rf/subscribe [:person pos])
        owed @(rf/subscribe [:owed name-value])]
    [:tr
     [:td
      [bs/form-control
       {:type :text
        :value name-value
        :on-change #(rf/dispatch [:update-person pos (.-target.value %)])}]]
     [:td (g-string/format "$%.02f" owed)]]))

(defn people-panel
  []
  (let [num-people @(rf/subscribe [:num-new-people])]
    [bs/panel {:header "People"}
     [bs/table
      [:thead
       [:tr
        [:th "Name"]
        [:th "Owes"]]]
      [:tbody
        (for [pos num-people]
          ^{:key (g-string/format "person-entry-%d" pos)}
          [person-entry pos])]]]))

(defn person-header
  [pos]
  (let [name-value @(rf/subscribe [:person pos])]
    [:th name-value]))

(defn item-checkbox
  [p-pos i-pos]
  (let [item-owned? @(rf/subscribe [:item-owned? p-pos i-pos])]
    [:td [bs/checkbox {:checked item-owned?
                       :on-change #(rf/dispatch [:update-owner
                                                 (.-target.checked %)
                                                 p-pos
                                                 i-pos])}]]))

(defn item-entry
  [i-pos num-people]
  (let [item-name @(rf/subscribe [:item-name i-pos])
        item-price @(rf/subscribe [:item-price i-pos])
        item-quantity @(rf/subscribe [:item-quantity i-pos])
        item-taxable @(rf/subscribe [:item-taxable i-pos])]
    [:tr
     [:td [bs/form-control
            {:type :text
             :value item-name
             :on-change #(rf/dispatch [:update-item (.-target.value %) i-pos :item-name])}]]
     [:td [bs/form-group
           {:validation-state (:valid-state item-price)}
           [bs/input-group
            [bs/input-group-addon "$"]
            [bs/form-control
             {:type :text
              :value (:value item-price)
              :disabled (:disabled? item-price)
              :on-change #(rf/dispatch [:update-item (.-target.value %) i-pos :item-price])}]
            [bs/form-control-feedback]]]]
     [:td [bs/form-group
           {:validation-state (:valid-state item-quantity)}
           [bs/form-control
            {:type :text
             :value (:value item-quantity)
             :disabled (:disabled? item-quantity)
             :on-change #(rf/dispatch [:update-item (.-target.value %) i-pos :item-quantity])}]
           [bs/form-control-feedback]]]
     [:td [bs/checkbox
            {:checked (:value item-taxable)
             :disabled (:disabled? item-taxable)
             :on-change #(rf/dispatch [:update-item (.-target.checked %) i-pos :item-taxable])}]]
     (for [p-pos num-people]
       ^{:key (g-string/format "row-%d-person-%d" i-pos p-pos)}
       [item-checkbox p-pos i-pos])]))


(defn items-panel
  []
  (let [num-people @(rf/subscribe [:num-existing-people])
        num-items @(rf/subscribe [:num-new-items])
        tax-rate @(rf/subscribe [:tax-rate-field])]
    [bs/panel {:header "Items"}
     [bs/table
      [:thead
       [:tr
        [:th "Item"]
        [:th "Price"]
        [:th "Quantity"]
        [:th "Taxable"]
        (for [p-pos num-people]
          ^{:key (g-string/format "items-person-%d" p-pos)}
          [person-header p-pos])]]
      [:tbody
       (for [i-pos num-items]
         ^{:key (g-string/format "item-row-%d" i-pos)}
         [item-entry i-pos num-people])
       [:tr
        [:td [:b "Tax Rate"]]
        [:td
         [bs/form-group
          {:validation-state (:valid-state tax-rate)}
          [bs/input-group
           [bs/form-control
            {:type :text
             :value (:value tax-rate)
             :on-change #(rf/dispatch [:update-tax-rate (.-target.value %)])}]
           [bs/input-group-addon "%"]
           [bs/form-control-feedback]]]]]]]]))

(defn main-panel []
  [:div
   [bs/navbar {:fluid true}
    [bs/navbar-header
     [bs/navbar-brand
      [:a "Split da Bill"]]]]
   [bs/grid {:fluid false}
    [bs/row
     [bs/col {:md 6}
      [people-panel]]]
    [bs/row
     [bs/col {:md 12}
      [items-panel]]]]])
