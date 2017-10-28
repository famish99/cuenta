(ns client.run
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frisk.core :refer [enable-re-frisk!]]
            [cuenta.events]
            [cuenta.subs]
            [cuenta.views :as views]
            [cuenta.config :as config]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (enable-re-frisk!)))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/router]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
