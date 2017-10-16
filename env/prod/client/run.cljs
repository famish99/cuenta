(ns client.run
  (:require [re-frame.core :as re-frame]
            [cuenta.events]
            [cuenta.subs]
            [cuenta.views :as views]
            [reagent.core :as reagent]))

(set! *print-fn* (fn [& _]))

(defn ^:export init
  []
  (re-frame/dispatch-sync [:initialize-db])
  (reagent/render [views/router]
                  (.getElementById js/document "app")))

(set! *main-cli-fn* init)
