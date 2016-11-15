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
    (update-in db [:config side :count] inc)
    (update-in [:config side :clicks] inc))))

(reg-event-db
 :update-stats
 (fn [db [_ stats]]
   (assoc-in db [:stats] stats)))

(reg-event-db
 :update-config
 (fn [db [_ config]]
   (assoc-in db [:config] config)))

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
     (update-in db [:config side :count] dec-count)))

(reg-event-db
 :request-updates
 (fn [db [_ side]]
   (do
     (cookiewars.core/send-transit-msg!
      (str {:cmd "update-stats"}))
     db)))


(reg-event-db
 :new-server-event
 (fn [db [_ event]]
   (let [ev (read-string event)
         cmd (:cmd ev)]
     (do
       ;; (println ":event" (read-string event))
       (case cmd
         "inc" (dispatch [:inc (:side ev)])
         "update-stats" (dispatch [:update-stats (:stats ev)])
         "update-config" (dispatch [:update-config (:config ev)]))
       db))))
