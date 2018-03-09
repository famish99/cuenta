(ns cuenta.views
  (:require [goog.string :as g-string]
            goog.string.format
            [re-frame.core :as rf]
            [cuenta.constants :as const]
            [cuenta.components.bootstrap :as bs]))

(defn person-entry
  [pos]
  (let [name-value @(rf/subscribe [:person pos])
        owed @(rf/subscribe [:owed name-value])]
    [:tr
     [:td
      [:input.form-control
       {:type :text
        :value name-value
        :on-blur #(rf/dispatch [:trim-person pos])
        :on-change #(rf/dispatch [:update-person pos (.-target.value %)])}]]
     [:td (g-string/format "$%.02f" owed)]]))

(defn people-panel
  []
  (let [count-people @(rf/subscribe [:count-new-people])
        claimed-total @(rf/subscribe [:claimed-total])
        total-cost @(rf/subscribe [:total-cost])]
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
     [:td.col-xs-4
      [:input.form-control
       {:type :text
        :value item-name
        :on-blur #(rf/dispatch [:cast-item :string "" i-pos :item-name])
        :on-change #(rf/dispatch [:update-item (.-target.value %) i-pos :item-name])}]]
     [:td.col-xs-2
      [bs/form-group
       {:validation-state (:valid-state item-price)}
       [bs/input-group
        [bs/input-group-addon "$"]
        [:input.form-control
         {:type :text
          :value (:value item-price)
          :disabled (:disabled? item-price)
          :on-blur #(rf/dispatch [:cast-item :money const/default-price i-pos :item-price])
          :on-change #(rf/dispatch [:update-item (.-target.value %) i-pos :item-price])}]]]]
     [:td.col-xs-1
      [bs/form-group
       {:validation-state (:valid-state item-quantity)}
       [:input.form-control
        {:type :text
         :value (:value item-quantity)
         :disabled (:disabled? item-quantity)
         :on-blur #(rf/dispatch [:cast-item :int "1" i-pos :item-quantity])
         :on-change #(rf/dispatch [:update-item (.-target.value %) i-pos :item-quantity])}]]]
     [:td
      [bs/checkbox
       {:checked (:value item-taxable)
        :disabled (:disabled? item-taxable)
        :on-change #(rf/dispatch [:update-item (.-target.checked %) i-pos :item-taxable])}]]
     (for [person people]
       ^{:key (g-string/format "row-%d-person-%s" i-pos person)}
       [item-checkbox person i-pos])]))

(defn tax-rate-field
  []
  (let [tax-rate @(rf/subscribe [:tax-rate-field])
        tax-amount @(rf/subscribe [:tax-amount])]
    [:tr
     [:td [bs/control-label "Tax Rate"]]
     [:td
      [bs/form-group
       {:validation-state (:valid-state tax-rate)}
       [bs/input-group
        [:input.form-control
         {:type :text
          :value (:value tax-rate)
          :on-blur #(rf/dispatch [:cast-tax-rate])
          :on-change #(rf/dispatch [:update-tax-rate (.-target.value %)])}]
        [bs/input-group-addon "%"]]]]
     [:td {:style {:padding-top "14px"}} [:b (g-string/format "= $%.02f" tax-amount)]]]))

(defn tip-amount-field
  []
  (let [tip-amount @(rf/subscribe [:tip-amount-field])]
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
          :on-blur #(rf/dispatch [:cast-tip-amount])
          :on-change #(rf/dispatch [:update-tip-amount (.-target.value %)])}]
        [bs/form-control-feedback]]]]]))

(defn total-cost-field
  []
  (let [total-cost @(rf/subscribe [:total-cost])]
    [:tr
     [:td [bs/form-group [bs/control-label "Ticket Total"]]]
     [:td {:style {:text-align :right}} [:b (g-string/format "$%.02f" total-cost)]]]))

(defn credit-to-field
  [people]
  [:tr
   [:td
    [bs/form-group
     [bs/control-label "Credit to"]]]
   [:td
    [bs/form-control
     {:component-class :select
      :disabled (< (count people) 1)
      :on-change #(rf/dispatch [:update-credit-to (.-target.value %)])}
     (for [person people]
       ^{:key (g-string/format "credit-select-%s" person)}
       [:option {:value person} person])]]])

(defn vendor-name-field
  []
  (let [vendor-name @(rf/subscribe [:vendor-name-field])]
    [:tr
     [:th
      [:input.form-control
       {:type :text
        :value vendor-name
        :on-change #(rf/dispatch [:update-vendor-name (.-target.value %)])}]]]))


(defn order-panel
  []
  (let [people @(rf/subscribe [:people])
        count-items @(rf/subscribe [:count-new-items])]
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
        (for [person people]
          ^{:key (g-string/format "items-person-%s" person)}
          [:th person])]]
      [:tbody
       (for [i-pos count-items]
         ^{:key (g-string/format "item-row-%d" i-pos)}
         [item-entry i-pos people])
       [tax-rate-field]
       [tip-amount-field]
       [total-cost-field]
       [credit-to-field people]
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
     [order-panel]]]])

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
            [:td {:style (if (= creditor-name debtor-name)
                           {:text-align :right :background :gray}
                           {:text-align :right})}
             (when-let [debt (get debts debtor-name)]
               (g-string/format "$%.02f" debt))])])]]]))

(defn transaction-entry
  [t-id]
  [:tr
   [:td @(rf/subscribe [:t-item-vendor t-id])]
   [:td @(rf/subscribe [:t-item-purchaser t-id])]
   [:td @(rf/subscribe [:t-item-cost t-id])]
   [:td @(rf/subscribe [:t-item-date t-id])]])

(defn recent-transactions []
  (let [t-list @(rf/subscribe [:recent-transactions])]
    [bs/panel {:header "Recent Transactions"}
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
         [bs/button {:bs-style :primary
                     :on-click #(rf/dispatch [:add-transaction])}
          [bs/glyphicon {:glyph :glyphicon-plus}] "Add Transaction"]]]]]]))

(defn home []
  [bs/grid {:fluid false}
   [bs/row
    [bs/col {:md 12}
     [people-matrix]]]
   [bs/row
    [bs/col {:md 12}
     [recent-transactions]]]])

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
                    :on-click #(rf/dispatch [:load-home])}
         "Split da Bill"]]]]
     [(get view-map route home)]]))
