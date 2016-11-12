(ns cookiewars.core
  (:require [ajax.core :refer [GET POST]]
            [cognitect.transit :as t]
            [cookiewars.ajax :refer [load-interceptors!]]
            cookiewars.handlers
            cookiewars.subscriptions
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [re-frame.core :as rf]
            [re-frisk.core :refer [enable-re-frisk!]]
            [reagent.core :as r]
            [secretary.core :as secretary])
  (:import goog.History))

(defn nav-link [uri title page collapsed?]
  (let [selected-page (rf/subscribe [:page])]
    [:li.nav-item
     {:class (when (= page @selected-page) "active")}
     [:a.nav-link
      {:href uri
       :on-click #(reset! collapsed? true)} title]]))

(defn navbar []
  (r/with-let [collapsed? (r/atom true)]
    [:nav.navbar.navbar-dark.bg-primary
     [:button.navbar-toggler.hidden-sm-up
      {:on-click #(swap! collapsed? not)} "☰"]
     [:div.collapse.navbar-toggleable-xs
      (when-not @collapsed? {:class "in"})
      [:a.navbar-brand {:href "#/"} "cookiewars"]
      [:ul.nav.navbar-nav
       [nav-link "#/" "Home" :home collapsed?]
       [nav-link "#/about" "About" :about collapsed?]]]]))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     "Cookiewars is real"]]])


;; ----------- ws stuff -------------------
;; ----------------------------------------
(defonce ws-chan (atom nil))

(def json-reader (t/reader :json))
(def json-writer (t/writer :json))

(defn send-transit-msg!
  [msg]
  (if @ws-chan
    (.send @ws-chan msg)
    (throw (js/Error. "Websocket is not available!"))))

;; (send-transit-msg! "test")

(defn make-websocket! [url]
  (println "attempting to connect websocket")
  (if-let [chan (js/WebSocket. url)]
    (do
      (set! (.-onmessage chan)
            (fn [msg] (rf/dispatch [:new-server-event (.-data msg)])))
      (reset! ws-chan chan)
      (println "Websocket connection established with: " url))
    (throw (js/Error. "Websocket connection failed!"))))


;; ----------- battle components ----------
;; ----------------------------------------
(defn battle-titles []
  (when-let [titles @(rf/subscribe [:titles])]
    [:div.row
     [:div.col-xs-6
      [:h3.text-xs-center
       (:left titles)]]
     [:div.col-xs-6
      [:h3.text-xs-center
       (:right titles)]]]))

(defonce timers [(js/setInterval #(rf/dispatch-sync [:tick :left]) 10)
                 (js/setInterval #(rf/dispatch-sync [:tick :right]) 10)
                 (js/setInterval #(rf/dispatch [:request-updates]) 500)])

;; (println @(rf/subscribe [:img :left]))

(defn participant [side]
  (let [img-url @(rf/subscribe [:img side])
        count @(rf/subscribe [:count side])
        clicks @(rf/subscribe [:clicks side])]

       [:div.container
        [:div.row
         [:div.text-xs-center
          [:img {:src img-url
                 :height 250
                 ;; :width 250
                 :on-drag-start #(.preventDefault %)
                 :on-mouse-down #(rf/dispatch [:click side])
                 }]]]

        [:div.row
         [:div.text-xs-center
          (str "clicks: " (int clicks) "!")]]

        [:div.row
         [:div.text-xs-center
          [:progress
           {:style {:width "80%"}
            :value count
            :max 70}]]]
        ]))

(defn battle-field []
  [:div.row
   [:div.col-xs-6
    [participant :left]]
   [:div.col-xs-6
    [participant :right]]])

(defn stat-comp []
  (let [stats @(rf/subscribe [:stats])
        online-count (:count stats)]
    [:div.row
     [:hr]
     [:div.col-xs-6
      [:h3 (str "Online: " online-count)]]]))

(defn battle-comp []
  (let [battle-title @(rf/subscribe [:battle-title])]
    [:div.container
     [:div.row [:h1.text-xs-center battle-title]]
     [battle-titles]
     [battle-field]
     [stat-comp]]))

;; ---------------------------------------

(defn home-page []
  [battle-comp])

(def pages
  {:home #'home-page
   :about #'about-page})

(defn page []
  [:div
   [navbar]
   [(pages @(rf/subscribe [:page]))]])

;; Routes
;; -------------------------
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (rf/dispatch [:set-active-page :home]))

(secretary/defroute "/about" []
  (rf/dispatch [:set-active-page :about]))

;; History
;; must be called after routes have been defined
;; -------------------------
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; Initialize app
;; -------------------------
(defn fetch-docs! []
  (GET (str js/context "/docs") {:handler #(rf/dispatch [:set-docs %])}))

(defn mount-components []
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (enable-re-frisk!)
  (load-interceptors!)
  ;; (fetch-docs!)
  (hook-browser-navigation!)
  (make-websocket! (str "ws://" (.-host js/location) "/ws"))
  (mount-components))
