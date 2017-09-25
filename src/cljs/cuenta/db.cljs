(ns cuenta.db)

(def transaction-defaults
  {:people []
   :items {}
   :tax-rate 8.25
   :owner-matrix {}})

(def default-db
  (conj {:route :home :owed-matrix {}} transaction-defaults))
   ;{"Huan" {"Allen" 12.02 "Grant" 3.24}
   ; "Allen" {"Grant" 5.27}
   ; "Grant" {"Zainil" 2.23}}
