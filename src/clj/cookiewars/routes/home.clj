(ns cookiewars.routes.home
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [compojure.core :refer [defroutes GET]]
            [cookiewars.layout :as layout]
            [org.httpkit.server :refer [on-close on-receive send! with-channel]]
            [ring.util.http-response :as response]))

(defn home-page []
  (layout/render "home.html"))

(def cookie-img "http://i3.istockimg.com/file_thumbview_approve/93368057/5/stock-illustration-93368057-chocolate-chip-cookie.jpg")

(def donut-img "http://rlv.zcache.com/kawaii_donut_delight_bold_colorful_sweet_sprinkles_round_pillow-rc199908d083d4f1e93b3bed867ad86ba_z6i0e_324.jpg")

(def competitions [(atom
                    {:stats {:users 0
                             :started false
                             :time-left 6
                             :clicks {:left 0 :right 0}
                             :update-at (java.util.Date.)}
                     :channels #{}
                     :config {:title "Decide"
                              :left {:title "First"
                                     :img cookie-img}
                              :right {:title "Second"
                                      :img donut-img}}})
                   ])

(def first-comp (first competitions))

;; (def competitions (atom init-competitions))



(defonce channels (atom #{}))
;; (send! (first @channels) (json/generate-string {:cmd "test"}))

(defn inc-clicks [e channel]
  (if (get-in @first-comp [:stats :started])
    (swap! first-comp update-in [:stats :clicks (:side e)] inc))
  (send! channel (str e)))

(defn send-stats [channel]
  (send! channel (str {:cmd "update-stats" :stats (:stats @first-comp)})))

(defn send-config [channel]
  (send! channel (str {:cmd "update-config" :config (:config @first-comp)})))

(defn decrease-timer []
  (Thread/sleep 1000)
  (swap! first-comp update-in [:stats :time-left] dec)
  (if (< 0 (get-in @first-comp [:stats :time-left]))
    (future (decrease-timer))
    (swap! first-comp assoc-in [:stats :started] false)))

(defn start-competition []
  (swap! first-comp assoc-in [:stats :started] true)
  (swap! first-comp assoc-in [:stats :update-at] (java.util.Date.))
  (future (decrease-timer)))

;; (start-competition)
;; (println channels)
;; (loop []
;; (map #(inc-clicks :right %) @channels)
;;   ;; (println "messages sended")
;;   (Thread/sleep 1000)
;;   (recur))

(defn connect! [channel]
  (println "channel opened:" channel)
  (swap! channels conj channel)
  (swap! first-comp update-in [:stats :users] inc))

(defn disconnect! [channel status]
  (println "channel closed:" status)
  (swap! channels #(remove #{channel} %))
  (swap! first-comp update-in [:stats :users] dec))

(defn handle-msg [msg channel]
  ;; (println "new message from client: " msg)
  (let [ev (clojure.edn/read-string msg)]
    (case (:cmd ev)
      "inc" (doseq [channel @channels]
              (inc-clicks ev channel))
      "update-stats" (send-stats channel)
      "update-config" (send-config channel)
      "start-competition" (start-competition))))

(defn ws-handler [request]
  (with-channel request channel
    (connect! channel)
    (on-close channel (partial disconnect! channel))
    (on-receive channel #(handle-msg % channel))))

(defroutes home-routes
  (GET "/" []
       (home-page))
  (GET "/ws" request (ws-handler request))
  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
       (response/header "Content-Type" "text/plain; charset=utf-8"))))

;; (do (cookiewars.core/-main) (cookiewars.figwheel/start-fw))
