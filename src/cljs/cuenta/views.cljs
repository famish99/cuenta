(ns cuenta.views
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]))

(defn title []
  (fn []
    [re-com/title
     :label "Split da Bill"
     :level :level1]))

(defn person-component
  [[id person-name]]
  (fn []
    [re-com/h-box
     :children
     [[re-com/label :label (str "Person " (inc id) ":")]
      [re-com/gap :size "1em"]
      [re-com/input-text
       :model person-name
       :on-change #(re-frame/dispatch [:update-person id %])
       :change-on-blur? false
       ]
      ]]
    ))

(defn people []
  (let [people (re-frame/subscribe [:people])]
    (fn []
      [re-com/v-box
       :children
       [[re-com/title
         :label "People"
         :level :level2]
        (for [person @people] [person-component person])]
       ])))

(defn main-panel []
  (fn []
    [re-com/v-box
     :height "100%"
     :children [[title] [people]]]))
