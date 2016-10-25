(ns cookiewars.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [cookiewars.ajax :refer [load-interceptors!]]
            [cookiewars.handlers]
            [cookiewars.subscriptions])
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


(defonce battle {:title "Decide cookiewarrior"
                 :left "Cookies"
                 :right "Candies"})

(defn battle-titles []
  (when-let [titles @(rf/subscribe [:titles])]
    [:div.row
     [:div.col-xs-6
      [:h3.text-xs-center
       (:left titles)]]
     [:div.col-xs-6
      [:h3.text-xs-center
       (:right titles)]]]))

(defn t-comp []
  (js/setInterval #(rf/dispatch-sync [:tick :left]) 1000)
  (js/setInterval #(rf/dispatch-sync [:tick :right]) 1000)
  [:div])

(defn participant [side]
  (let [img-url @(rf/subscribe [:img])
        count @(rf/subscribe [:count side])]
      [:div.col-xs-6
       [:div.container
        [:div.row
         [:div.text-xs-center
          [:img {:src img-url
                 :on-click #(rf/dispatch [:click side])}]
          ]]

        [:div.row
         [:div.text-xs-center
          (str "count: " count "!")]]
        [:div.row
         [:div.text-xs-center
          [:progress.porgress-striped.progress-info.progress-animated
           {:style {:width "80%"}
            :value count
            :max 25}
           ]]]]])
  )


(defn battle-field []
  [:div.row
   [participant :left]
   [participant :right]
   ])

(defn battle-comp []
  [:div.container
   [:div.row [:h1.text-xs-center (:title battle)]]
   [battle-titles]
   [battle-field]
   [t-comp]])

(defn home-page []
  [battle-comp])

(def pages
  {:home #'home-page
   :about #'about-page})

(defn page []
  [:div
   [navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (rf/dispatch [:set-active-page :home]))

(secretary/defroute "/about" []
  (rf/dispatch [:set-active-page :about]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET (str js/context "/docs") {:handler #(rf/dispatch [:set-docs %])}))

(defn mount-components []
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
