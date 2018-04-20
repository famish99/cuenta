(ns cuenta.routes)

(def route-map
  ["/" {#{"" "index"} :index
        [[#"(css|js|static)" :resource-type]] {[[#".*" :resource-name]] :static-resource}
        "api/" {"load/" {"matrix" :load-matrix
                         "items" :load-items
                         "people" :load-people
                         "vendors" :load-vendors
                         ["transaction/" [#"\d+" :transaction-id]] :load-transaction
                         "transactions" :load-transactions}
                "save/" {"transaction" :save-transaction}}
        "loopback" :loopback}])
