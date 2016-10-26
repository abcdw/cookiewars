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

(defonce channels (atom #{}))
;; (send! (first @channels) (json/generate-string {:cmd "test"}))

(defn inc-clicks [side channel]
  (send! channel (str {:cmd "inc" :side side})))

;; (println channels)
;; (loop []
;; (map #(inc-clicks :right %) @channels)
;;   ;; (println "messages sended")
;;   (Thread/sleep 1000)
;;   (recur))

(defn connect! [channel]
  (println "channel open" channel)
  (swap! channels conj channel))

(defn disconnect! [channel status]
  (println "channel closed:" status)
  (swap! channels #(remove #{channel} %)))

(defn handle-msg [msg]
  (println "new message: " msg)
  (let [side (:side (clojure.edn/read-string msg))]
    (doseq [channel @channels]
      (inc-clicks side channel))))

(defn ws-handler [request]
  (with-channel request channel
    (connect! channel)
    (on-close channel (partial disconnect! channel))
    (on-receive channel #(handle-msg %))))

(defroutes home-routes
  (GET "/" []
       (home-page))
  (GET "/ws" request (ws-handler request))
  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
       (response/header "Content-Type" "text/plain; charset=utf-8"))))
