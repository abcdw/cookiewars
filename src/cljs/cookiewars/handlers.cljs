(ns cookiewars.handlers
  (:require [cookiewars.db :as db]
            [re-frame.core :refer [dispatch reg-event-db]]))

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :set-active-page
  (fn [db [_ page]]
    (assoc db :page page)))

(reg-event-db
  :set-docs
  (fn [db [_ docs]]
    (assoc db :docs docs)))

(reg-event-db
 :click
 (fn [db [_ participant]]
     (update-in db [:battle participant :count] inc)))


(defn dec-count [count]
  (if (pos? count)
    (- count (/ (* count 25) 100))
    0))

(reg-event-db
 :tick
 (fn [db [_ participant]]
   (update-in db [:battle participant :count] dec-count)
   ))
