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
            [secretary.core :as secretary]
            goog.string.format)
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

(def sub-form
  " <!-- Begin MailChimp Signup Form -->
<link href=\"//cdn-images.mailchimp.com/embedcode/classic-10_7.css\" rel=\"stylesheet\" type=\"text/css\">
<style type=\"text/css\">
	#mc_embed_signup{background:#fff; clear:left; font:14px Helvetica,Arial,sans-serif; }
	/* Add your own MailChimp form style overrides in your site stylesheet or in this style block.
	   We recommend moving this block and the preceding CSS link to the HEAD of your HTML file. */
</style>
<div id=\"mc_embed_signup\">
<form action=\"//rednot.us10.list-manage.com/subscribe/post?u=5b4862b4e4a1d13ad46abc221&amp;id=4b9d6df08f\" method=\"post\" id=\"mc-embedded-subscribe-form\" name=\"mc-embedded-subscribe-form\" class=\"validate\" target=\"_blank\" novalidate>
    <div id=\"mc_embed_signup_scroll\">
	<h2>Get latest news about our project</h2>
<div class=\"indicates-required\"><span class=\"asterisk\">*</span> indicates required</div>
<div class=\"mc-field-group\">
	<label for=\"mce-EMAIL\">Email Address  <span class=\"asterisk\">*</span>
</label>
	<input type=\"email\" value=\"\" name=\"EMAIL\" class=\"required email\" id=\"mce-EMAIL\">
</div>
<div class=\"mc-field-group\">
	<label for=\"mce-FNAME\">First Name </label>
	<input type=\"text\" value=\"\" name=\"FNAME\" class=\"\" id=\"mce-FNAME\">
</div>
<div class=\"mc-field-group\">
	<label for=\"mce-LNAME\">Last Name </label>
	<input type=\"text\" value=\"\" name=\"LNAME\" class=\"\" id=\"mce-LNAME\">
</div>
	<div id=\"mce-responses\" class=\"clear\">
		<div class=\"response\" id=\"mce-error-response\" style=\"display:none\"></div>
		<div class=\"response\" id=\"mce-success-response\" style=\"display:none\"></div>
	</div>    <!-- real people should not fill this in and expect good things - do not remove this or risk form bot signups-->
    <div style=\"position: absolute; left: -5000px;\" aria-hidden=\"true\"><input type=\"text\" name=\"b_5b4862b4e4a1d13ad46abc221_4b9d6df08f\" tabindex=\"-1\" value=\"\"></div>
    <div class=\"clear\"><input type=\"submit\" value=\"Subscribe\" name=\"subscribe\" id=\"mc-embedded-subscribe\" class=\"button\"></div>
    </div>
</form>
</div>
<script type='text/javascript' src='//s3.amazonaws.com/downloads.mailchimp.com/js/mc-validate.js'></script><script type='text/javascript'>(function($) {window.fnames = new Array(); window.ftypes = new Array();fnames[0]='EMAIL';ftypes[0]='email';fnames[1]='FNAME';ftypes[1]='text';fnames[2]='LNAME';ftypes[2]='text';}(jQuery));var $mcj = jQuery.noConflict(true);</script>
<!--End mc_embed_signup-->")

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:h3.text-xs-center "Cookiewars is real"]]]
   [:div {:dangerouslySetInnerHTML {:__html sub-form}}]
   #_[:div.row [:a {:href "http://eepurl.com/couoTX"} "Subscribe"] " for news about project."]])


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

(def nb (r/atom []))
(defn participant [side]
  (let [img-url @(rf/subscribe [:img side])
        count @(rf/subscribe [:count side])
        title @(rf/subscribe [:title side])
        clicks @(rf/subscribe [:clicks side])
        local-clicks @(rf/subscribe [:local-clicks side])
        animation-type @(rf/subscribe [:animation])]

       [:div.container
        [:div.row
         [:h3.text-xs-center title]]
        [:div.row
         [:h3.text-xs-center clicks]]
        [:div.row
         [:div.text-xs-center
          [:img
           {:src img-url
            :id side
            :height 250
            ;; :width 250
            :on-drag-start #(.preventDefault %)
            ;; :on-mouse-down #(rf/dispatch [:click side])
            :on-mouse-down (fn [e] (let [target (.-target e)
                                   rect (.getBoundingClientRect target)
                                   abs-pt {:x (.-clientX e)
                                           :y (.-clientY e)}
                                   rel-pt {:x (- (.-clientX e) (.-left rect))
                                           :y (- (.-clientY e) (.-top rect))}
                                   ;; oth-pt {:x (.-screenX e)
                                   ;;         :y (.-screenY e)}
                                        ]
                                    ;; (println oth-pt
                                    ;;          abs-pt)
                                    ;; (swap! nb conj abs-pt)
                                    (rf/dispatch [:click side {:pt rel-pt :tp animation-type}])))
            }]]]

        [:div.row
         [:div.text-xs-center
          (str "your clicks: " (int local-clicks) "!")]]

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
        {:keys [users time-left started]} stats
        seconds (goog.string.format "%02d" (mod time-left 60))
        minutes (quot time-left 60)
        status-line (if (= 0 time-left)
                      "Finished"
                      (str "Remaining time: "
                           (if (not started)
                             "not started"
                             (str minutes ":" seconds))))]
    [:div.container
     ;; [:hr]
     [:div.row
      [:div.col-xs-12
       [:h5.text-xs-center status-line]]
      [:div.row [:hr]]
      ;; [:div.col-xs-6
      ;;  [:h3 (str "Online: " users)]]
      ]
     ;; [:div.row [:hr]]
     ]))

(def cls (r/atom "main-bubble scale-small"))


;; (defn nbubble []
;;   [:div.new-bubble])

(defn nbubble [elem]
  (let [img (. js/document getElementById (str (name (:side elem))))
        rect (.getBoundingClientRect img)
        x (+ (.-left rect) (-> elem :pt :x))
        y (+ (.-top rect) (-> elem :pt :y))]
    [:div {:class (case (:tp elem)
                    2 "admin-animation"
                    "bubble-animation")
           :style {:top y
                   :left x}}]))

;; (do (reset! nb []) (swap! nb conj {:x 10 :y 10}))

(defn animation-comp []
  (let [nb @(rf/subscribe [:anim-elems])]
    [:div.bubble-container
     (for [[i elem] (map-indexed vector nb)]
       ^{:key i} [nbubble elem])]))

(defn start-comp []
  (let [anim-type @(rf/subscribe [:animation])]
    (if (= anim-type 2)
      [:div.row
       [:button.btn {:on-click #(send-transit-msg! (str {:cmd "start-competition"}))} "start"]
       [:button.btn {:on-click #(send-transit-msg! (str {:cmd "restart-competition"}))} "restart"]])
    ))

(defn battle-comp []
  (let [battle-title @(rf/subscribe [:battle-title])]
    [:div.container
     [animation-comp]
     [:div.row [:h1.text-xs-center battle-title]]
     [stat-comp]
     ;; [battle-titles]
     [battle-field]
     [start-comp]]))

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

(secretary/defroute "/adm" []
  (rf/dispatch [:set-active-page :home])
  (rf/dispatch [:set-animation 2]))

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
