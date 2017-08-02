(ns cuenta.views
  (:require [re-frame.core :as rf]
            [re-com.core :as re-com]))

(defn title []
  (fn []
    [re-com/title
     :label "Split da Bill"
     :level :level1]))

;(defn person-component
;  [[id person-name]]
;  (fn []
;    [re-com/h-box
;     :children
;     [[re-com/label :label (str "Person " (inc id) ":")]
;      [re-com/gap :size "1em"]
;      [re-com/input-text
;       :model person-name
;       :on-change #(rf/dispatch [:update-person id %])
;       ]]]
;    ))

(defn show-people []
  (let [people @(rf/subscribe [:people])]
    [:div (for [person people] [:p person])]))

(defn people []
  (let [people @(rf/subscribe [:people])]
    [:div
     [re-com/v-box
      :children
      [[re-com/title
        :label "People"
        :level :level2]
       [re-com/button
        :label "Add Person"
        :on-click #(rf/dispatch [:ap-modal true])]
       [re-com/box
        :child [show-people]]]]
     ]))

(defn ap-modal []
  (fn []
    [re-com/modal-panel
     :child [re-com/v-box
             :children [[re-com/box
                         :margin "10px"
                         :child [:h3 "Add Person"]]
                        [re-com/box
                         :margin "10px"
                         :child [:input.form-control
                                 {:on-change #(rf/dispatch [:update-person  (-> % .-target .-value)])}]]
                        [re-com/h-box
                         :children [[re-com/box
                                     :margin "10px"
                                     :child [:button.btn.btn-default {:on-click #(rf/dispatch [:add-person])} "Add"]]
                                    [re-com/box
                                     :margin "10px"
                                     :child [:button.btn.btn-default {:on-click #(rf/dispatch [:ap-modal false])} "Close"]]]]]]]))

(defn main-panel []
  (let [mod-viewable? @(rf/subscribe [:ap-modal])]
    [re-com/v-box
     :height "100%"
     :children [[title]
                [people]
                (when mod-viewable?
                  [ap-modal])]]))
