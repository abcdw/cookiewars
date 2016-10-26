(ns cookiewars.handlers
  (:require [cljs.reader :refer [read-string]]
            [cookiewars.db :as db]
            [re-frame.core :refer [dispatch reg-event-db]]))

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

;; (reg-event-db
;;  :init-ws
;;  (fn [db [_ url]]
;;    (do
;;      (println url)
;;      db)))

(reg-event-db
  :set-active-page
  (fn [db [_ page]]
    (assoc db :page page)))

(reg-event-db
  :set-docs
  (fn [db [_ docs]]
    (assoc db :docs docs)))

(reg-event-db
 :inc
 (fn [db [_ side]]
   (->
    (update-in db [:battle side :count] inc)
    (update-in [:battle side :clicks] inc)
    )
   ))

(reg-event-db
 :click
 (fn [db [_ side]]
   (do
     ;; (println side " clicked")
     (cookiewars.core/send-transit-msg!
      (str {:cmd "inc"
            :side side}))
     db)))

(defn dec-count [count]
  (if (pos? count)
    (- count (/ (* count 0.3) 100))
    0))

(reg-event-db
 :tick
 (fn [db [_ side]]
   (update-in db [:battle side :count] dec-count)
   ))

(reg-event-db
 :new-server-event
 (fn [db [_ event]]
   (let [{:keys [cmd side]} (read-string event)]
     (do
       (println ":event" (read-string event))
       (if (= cmd "inc")
           (dispatch [:inc side]))
       db))))
