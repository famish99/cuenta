(ns cuenta.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [cuenta.events]
            [cuenta.subs]
            [cuenta.views :as views]))

(defn mount-root []
  (reagent/render [views/router]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (mount-root))
