(ns cuenta.erase-debt.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-fx
  ::erase-debt
  (fn [world _]
    {:db (-> world
             :db
             (assoc :route :erase-debt
                    ::selected-cells #{}))}))

(rf/reg-event-fx
  ::save-erase
  (fn [{:keys [db]} _]
    {:api {:action :erase-debt
           :params {:selected-cells (::selected-cells db)}
           :on-success [:load-home]
           :on-failure [:dump-error]}}))

(defn toggle-item
  [input-set item]
  ((if (contains? input-set item) disj conj) input-set item))

(rf/reg-event-db
  ::select-cell
  (fn [db [_ creditor debtor]]
    (update db ::selected-cells toggle-item [creditor debtor])))
