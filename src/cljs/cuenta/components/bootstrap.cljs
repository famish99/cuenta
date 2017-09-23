(ns cuenta.components.bootstrap
  (:require [cljsjs.react-bootstrap]
            [reagent.core :as r]))

(def checkbox (r/adapt-react-class (.-Checkbox js/ReactBootstrap)))
(def col (r/adapt-react-class (.-Col js/ReactBootstrap)))
(def control-label (.-ControlLabel js/ReactBootstrap))
(def form (r/adapt-react-class (.-Form js/ReactBootstrap)))
(def form-control (r/adapt-react-class (.-FormControl js/ReactBootstrap)))
(def form-control-feedback (r/adapt-react-class (.-FormControl.Feedback js/ReactBootstrap)))
(def form-group (r/adapt-react-class (.-FormGroup js/ReactBootstrap)))
(def grid (r/adapt-react-class (.-Grid js/ReactBootstrap)))
(def input-group (r/adapt-react-class (.-InputGroup js/ReactBootstrap)))
(def input-group-addon (r/adapt-react-class (.-InputGroup.Addon js/ReactBootstrap)))
(def navbar (r/adapt-react-class (.-Navbar js/ReactBootstrap)))
(def navbar-header (r/adapt-react-class (.-Navbar.Header js/ReactBootstrap)))
(def navbar-brand (r/adapt-react-class (.-Navbar.Brand js/ReactBootstrap)))
(def panel (r/adapt-react-class (.-Panel js/ReactBootstrap)))
(def row (r/adapt-react-class (.-Row js/ReactBootstrap)))
(def table (r/adapt-react-class (.-Table js/ReactBootstrap)))
