(ns cuenta.routes)

(def route-map
  ["/" {#{"" "index"} :index
        [[#"(css|js|static)" :resource-type]] {[[#".*" :resource-name]] :static-internal}
        "api/load/matrix" :load-matrix
        ["api/load/transaction/" [#"\d+" :transaction-id]] :load-transaction
        "api/load/transactions" :load-transactions
        "api/save/transaction" :save-transaction
        "loopback" :loopback}])
