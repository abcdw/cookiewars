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
   {:left (get-in db [:battle :left :title])
    :right (get-in db [:battle :right :title])}))

(reg-sub
 :img
 (fn [db [_ side]]
   (get-in db [:battle side :img])))

(reg-sub
 :battle-title
 (fn [db _]
   (get-in db [:battle :title])))

(reg-sub
 :count
 (fn [db [_ participant]]
   (get-in db [:battle participant :count])))

(reg-sub
 :clicks
 (fn [db [_ participant]]
   (get-in db [:battle participant :clicks])))

(reg-sub
 :stats
 (fn [db _]
   (get-in db [:stats])))
