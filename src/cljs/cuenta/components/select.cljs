(ns cuenta.components.select
  (:require [cljsjs.react-select]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(def async (r/adapt-react-class (.-Async js/Select)))
(def create (r/adapt-react-class (.-Creatable js/Select)))
(def select (r/adapt-react-class js/Select))
