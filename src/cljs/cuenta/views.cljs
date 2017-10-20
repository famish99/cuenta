(ns cuenta.views
  (:require [goog.string :as g-string]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [re-com.core :as re-com]
            [cuenta.constants :as const]
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
  (let [count-people @(rf/subscribe [:count-new-people])]
    [bs/panel {:header "People"}
     [bs/table
      [:thead
       [:tr
        [:th "Name"]
        [:th "Owes"]]]
      [:tbody
       (for [pos count-people]
         ^{:key (g-string/format "person-entry-%d" pos)}
         [person-entry pos])]]]))

(defn item-checkbox
  [person i-pos]
  (let [item-owned? @(rf/subscribe [:item-owned? person i-pos])]
    [:td [bs/checkbox {:checked item-owned?
                       :on-change #(rf/dispatch [:update-owner
                                                 (.-target.checked %)
                                                 person
                                                 i-pos])}]]))

(defn item-entry
  [i-pos people]
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
              :on-blur #(rf/dispatch [:cast-item :money const/default-price i-pos :item-price])
              :on-change #(rf/dispatch [:update-item (.-target.value %) i-pos :item-price])}]
            [bs/form-control-feedback]]]]
     [:td [bs/form-group
           {:validation-state (:valid-state item-quantity)}
           [bs/form-control
            {:type :text
             :value (:value item-quantity)
             :disabled (:disabled? item-quantity)
             :on-blur #(rf/dispatch [:cast-item :int "1" i-pos :item-quantity])
             :on-change #(rf/dispatch [:update-item (.-target.value %) i-pos :item-quantity])}]
           [bs/form-control-feedback]]]
     [:td [bs/checkbox
           {:checked (:value item-taxable)
            :disabled (:disabled? item-taxable)
            :on-change #(rf/dispatch [:update-item (.-target.checked %) i-pos :item-taxable])}]]
     (for [person people]
       ^{:key (g-string/format "row-%d-person-%s" i-pos person)}
       [item-checkbox person i-pos])]))


(defn items-panel
  []
  (let [people @(rf/subscribe [:people])
        count-items @(rf/subscribe [:count-new-items])
        tax-rate @(rf/subscribe [:tax-rate-field])]
    [bs/panel {:header "Items"}
     [bs/table
      [:thead
       [:tr
        [:th "Item"]
        [:th "Price"]
        [:th "Quantity"]
        [:th "Taxable"]
        (for [person people]
          ^{:key (g-string/format "items-person-%s" person)}
          [:th person])]]
      [:tbody
       (for [i-pos count-items]
         ^{:key (g-string/format "item-row-%d" i-pos)}
         [item-entry i-pos people])
       [:tr
        [:td [bs/control-label "Tax Rate"]]
        [:td
         [bs/form-group
           {:validation-state (:valid-state tax-rate)}
          [bs/input-group
           [bs/form-control
            {:type :text
             :value (:value tax-rate)
             :on-blur #(rf/dispatch [:cast-tax-rate])
             :on-change #(rf/dispatch [:update-tax-rate (.-target.value %)])}]
           [bs/input-group-addon "%"]
           [bs/form-control-feedback]]]]]
       [:tr
        [:td
         [bs/form-group
          [bs/control-label "Credit to:"]]]
        [:td
         [bs/form-control {:component-class :select
                           :disabled (< (count people) 1)
                           :on-change #(rf/dispatch [:update-credit-to (.-target.value %)])}
          (for [person people]
            ^{:key (g-string/format "credit-select-%s" person)}
            [:option {:value person} person])]]]
       [:tr
        [:td {:col-span 2}
         [bs/button {:bs-style :primary
                     :on-click #(rf/dispatch [:save-transaction])}
          [bs/glyphicon {:glyph :glyphicon-floppy-disk}] "Save"]]]]]]))

(defn transaction-view []
  [bs/grid {:fluid false}
   [bs/row
    [bs/col {:md 6}
     [people-panel]]]
   [bs/row
    [bs/col {:md 12}
     [items-panel]]]])

(defn people-matrix []
  (let [owed-matrix @(rf/subscribe [:owed-matrix])
        owed-cols @(rf/subscribe [:owed-cols])]
    [bs/panel {:header "Debt Matrix"}
     [bs/table
      [:thead
       [:tr
        [:th "Creditor"]
        (for [debtor-name owed-cols]
          ^{:key (g-string/format "debtor-header-%s" debtor-name)}
          [:th {:style {:text-align :right}} debtor-name])]]
      [:tbody
       (for [[creditor-name debts] owed-matrix]
         ^{:key (g-string/format "debt-row-%s" creditor-name)}
         [:tr
          [:td creditor-name]
          (for [debtor-name owed-cols]
            ^{:key (g-string/format "%s-owes-%s" creditor-name debtor-name)}
            [:td {:style {:text-align :right}}
             (g-string/format "$%.02f" (get debts debtor-name 0))])])
       [:tr
        [:td
         [bs/button {:bs-style :primary
                     :on-click #(rf/dispatch [:add-transaction])}
          [bs/glyphicon {:glyph :glyphicon-plus}] "Add Transaction"]]]]]]))

(defn home []
  [bs/grid {:fluid false}
   [bs/row
    [bs/col {:md 12}
     [people-matrix]]]])

(def view-map
  {:home home
   :transaction transaction-view})

(defn router
  []
  (let [route @(rf/subscribe [:route])]
    [:div
     [bs/navbar {:fluid true}
      [bs/navbar-header
       [bs/navbar-brand
        [bs/button {:bs-style :link
                    :on-click #(rf/dispatch [:update-route :home])}
         "Split da Bill"]]]
      [bs/navbar-collapse
       [bs/nav {:pull-right true}
        [bs/nav-item {:event-key "dump"
                      :on-click #(rf/dispatch [:dump-backend])} "DUMP ME"]]]]
     [(get view-map route home)]]))
