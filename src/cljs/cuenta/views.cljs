(ns cuenta.views
  (:require [goog.string :as g-string]
            goog.string.format
            [re-frame.core :as rf]
            [cuenta.constants :as const]
            [cuenta.util :as util]
            [cuenta.components.bootstrap :as bs]
            [cuenta.components.select :as sel]))

(defn owed-field
  [pos]
  [:td (g-string/format "$%.02f" @(rf/subscribe [:owed pos]))])

(defn person-field
  [pos]
  [:td
    [sel/create
     {:value @(rf/subscribe [:person pos])
      :on-change #(rf/dispatch [:update-person pos (some-> % .-value)])
      :options @(rf/subscribe [:person-suggest pos])
      :ignore-case false}]])

(defn person-entry
  [pos]
  [:tr
   [person-field pos]
   [owed-field pos]])

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
  [user-value i-pos]
  [:td
   [bs/checkbox
    {:checked @(rf/subscribe [:item-owned? user-value i-pos])
     :on-change #(rf/dispatch [:update-owner (.-target.checked %) user-value i-pos])}]])

(defn item-entry
  [i-pos people]
  (let [item-price @(rf/subscribe [:item-price i-pos])
        item-quantity @(rf/subscribe [:item-quantity i-pos])
        item-taxable @(rf/subscribe [:item-taxable i-pos])]
    [:tr
     [:td.col-xs-4
      [sel/create
       {:value @(rf/subscribe [:item-name i-pos])
        :on-change #(rf/dispatch [:update-item-name (some-> % .-value) i-pos])
        :options @(rf/subscribe [:item-suggest i-pos])
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
          :on-change #(rf/dispatch [:update-tip-amount (.-target.value %)])}]]]]]))

(defn total-cost-field
  []
  (let [total-cost @(rf/subscribe [:total-cost])]
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
      :disabled @(rf/subscribe [:credit-to-disabled])
      :value @(rf/subscribe [:credit-to-value])
      :on-change #(rf/dispatch [:update-credit-to (some-> % .-value)])
      :options @(rf/subscribe [:credit-to-suggest])}]]])

(defn vendor-name-field
  []
  [:tr
   [:th
    [sel/create
     {:value @(rf/subscribe [:vendor-name-field])
      :on-change #(rf/dispatch [:update-vendor-name (some-> % .-value)])
      :options @(rf/subscribe [:vendor-suggest])
      :ignore-case false}]]])

(defn person-header
  [p-id]
  [:th @(rf/subscribe [:person-field p-id])])

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
                     :on-click #(rf/dispatch [:save-transaction])}
          [bs/glyphicon {:glyph :floppy-disk}] " Save"]]]]]]))

(defn add-transaction []
  [bs/grid {:fluid false}
   [bs/row
    [bs/col {:md 6}
     [people-panel]]]
   [bs/row
    [bs/col {:md 12}
     [order-panel]]]])

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
         [credit-row owed-cols creditor debts])]]]))

(defn transaction-entry
  [t-id]
  [:tr
   [:td [:a.make-link
         {:on-click #(rf/dispatch [:view-transaction t-id])
          :tab-index 0}
         @(rf/subscribe [:t-item-vendor t-id])]]
   [:td @(rf/subscribe [:t-item-purchaser t-id])]
   [:td @(rf/subscribe [:t-item-cost t-id])]
   [:td @(rf/subscribe [:t-item-date t-id])]])

(defn recent-transactions []
  (let [t-list @(rf/subscribe [:recent-transactions])]
    [:div.panel.panel-default
     [:div.panel-heading
      [:a.make-link {:on-click #(rf/dispatch [:transaction-list])
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
          [bs/button {:bs-style :primary
                      :on-click #(rf/dispatch [:add-transaction])}
           [bs/glyphicon {:glyph :plus}] " Add Transaction"]]]]]]]))

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

(defn view-transactions []
  (let [t-list @(rf/subscribe [:transaction-list])
        curr-page @(rf/subscribe [:curr-t-page])
        page-count @(rf/subscribe [:tot-t-page])]
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
           :on-click #(rf/dispatch [:prev-transaction-page])}
          bs/left-arrow " Prev"]]
        [:li
         [:span {:style {:border :none}}
          (g-string/format "%d of %d" curr-page page-count)]]
        [:li.next
         (when (= curr-page page-count) {:style {:visibility :hidden}})
         [:a.make-link
          {:tab-index 0
           :on-click #(rf/dispatch [:next-transaction-page])}
          "Next " bs/right-arrow]]]]]]))

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
   :add-transaction add-transaction
   :view-transaction view-transaction
   :view-transactions view-transactions})

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
