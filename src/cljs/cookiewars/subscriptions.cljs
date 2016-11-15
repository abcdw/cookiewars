(ns cookiewars.subscriptions
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :page
 (fn [db _]
   (:page db)))

(reg-sub
 :docs
 (fn [db _]
   (:docs db)))

(reg-sub
 :titles
 (fn [db _]
   {:left (get-in db [:config :left :title])
    :right (get-in db [:config :right :title])}))

(reg-sub
 :title
 (fn [db [_ side]]
    (get-in db [:config side :title])))

(reg-sub
 :img
 (fn [db [_ side]]
   (get-in db [:config side :img])))

(reg-sub
 :battle-title
 (fn [db _]
   (get-in db [:config :title])))

(reg-sub
 :count
 (fn [db [_ participant]]
   (get-in db [:config participant :count])))

(reg-sub
 :clicks
 (fn [db [_ participant]]
   (get-in db [:stats :clicks participant])))

(reg-sub
 :local-clicks
 (fn [db [_ participant]]
   (get-in db [:config participant :clicks])))

(reg-sub
 :stats
 (fn [db _]
   (get-in db [:stats])))
