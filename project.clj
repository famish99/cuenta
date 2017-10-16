(defproject cuenta "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [cljs-ajax "0.7.2"]
                 [com.datomic/datomic-free "0.9.5561"]
                 [day8.re-frame/http-fx "0.1.4"]
                 [figwheel-sidecar "0.5.9"]
                 [reagent "0.6.2"]
                 [cljsjs/react-bootstrap "0.31.0-0" :exclusions [cljsjs/react]]
                 [re-frame "0.9.4"]
                 [re-frisk "0.4.5"]
                 [org.clojure/core.async "0.2.391"]
                 [ring "1.6.0"]
                 [re-com "2.0.0"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.9"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:http-server-root "public"
             :server-port 3460
             :nrepl-port 7003
             :ring-handler cuenta.core/backend-handler
             :server-logfile false
             :css-dirs ["resources/public/css"]}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.8.2"]
                   [com.cemerick/piggieback "0.2.2"]
                   [org.clojure/tools.nrepl "0.2.10"]]
    :plugins      [[lein-figwheel "0.5.9"]]}}


  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "cuenta.core/mount-root"
                    :websocket-host :js-client-host}
     :compiler     {:main                 cuenta.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :parallel-build       true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}}}


    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            cuenta.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]})





