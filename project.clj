(defproject cuenta "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [bidi "2.0.12"]
                 [cljs-ajax "0.7.2"]
                 [com.cognitect/transit-clj "0.8.300"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [com.layerware/hugsql "0.4.8"]
                 [org.clojure/java.jdbc "0.7.3"]
                 [migratus "1.0.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.9"]
                 [mysql/mysql-connector-java "8.0.8-dmr"]
                 [day8.re-frame/http-fx "0.1.4"]
                 [cljsjs/react-bootstrap "0.31.0-0" :exclusions [cljsjs/react]]
                 [org.clojure/core.async "0.2.391"]
                 [org.immutant/web "2.1.9"]
                 [reagent "0.6.2"]
                 [re-frame "0.9.4"]
                 [ring "1.6.0"]
                 [ring/ring-defaults "0.3.1"]]

  :plugins [[lein-cljsbuild "1.1.4"]
            [lein-figwheel "0.5.9"]
            [migratus-lein "0.5.3"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources/public/"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:http-server-root "public"
             :server-port 3460
             :nrepl-port 7003
             :ring-handler cuenta.core/backend-handler
             :server-logfile false
             :css-dirs ["resources/public/css"]}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :migratus {:store :database
             :migration-dir "migrations"
             :db ~(get (System/getenv) "DATABASE_URL")}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.8.2"]
                   [com.cemerick/piggieback "0.2.2"]
                   [figwheel-sidecar "0.5.9"
                    :exclusions [org.clojure/tools.nrepl
                                 ring/ring-core
                                 commons-codec]]
                   [re-frisk "0.4.5" :exclusions [com.google.guava/guava]]
                   [org.clojure/tools.nrepl "0.2.10"]]
    :source-paths ["src/clj" "src/cljc" "env/dev"]}
   :prod
   {:jvm-opts ^:replace ["-Xmx2g" "-server"]
    :source-paths ["src/clj" "src/cljc" "env/prod"]
    :prep-tasks ["deps"
                 ["cljsbuild" "once" "min"]
                 ["migratus" "migrate"]]
    :env {:production true}
    :aot :all
    :omit-source true
    :main backend/main}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["env/dev" "src/cljs" "src/cljc"]
     :figwheel     {:on-jsload client.run/mount-root
                    :websocket-host :js-client-host}
     :compiler     {:main                 client.run
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :parallel-build       true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}}}


    {:id           "min"
     :source-paths ["env/prod" "src/cljs" "src/cljc"]
     :compiler     {:main            client.run
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :parallel-build       true
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]})

