(ns client.run
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frisk.core :refer [enable-re-frisk!]]
            [cuenta.events]
            [cuenta.subs]
            [cuenta.views :as views]
            [cuenta.config :as config]))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (enable-re-frisk!)
  (reagent/render [views/router]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (enable-console-print!)
  (mount-root))
