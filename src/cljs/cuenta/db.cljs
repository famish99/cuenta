(ns cuenta.db)

(def default-db
  {:route :home
   :owed-matrix {"Huan" {"Allen" 12.02 "Grant" 3.24}
                 "Allen" {"Grant" 5.27}
                 "Grant" {"Zainil" 2.23}}
   :people []
   :items {}
   :tax-rate 8.25
   :owner-matrix {}})
