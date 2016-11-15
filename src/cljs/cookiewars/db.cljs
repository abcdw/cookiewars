(ns cookiewars.db)

(def cookie-img "http://i3.istockimg.com/file_thumbview_approve/93368057/5/stock-illustration-93368057-chocolate-chip-cookie.jpg")

(def donut-img "http://rlv.zcache.com/kawaii_donut_delight_bold_colorful_sweet_sprinkles_round_pillow-rc199908d083d4f1e93b3bed867ad86ba_z6i0e_324.jpg")
;; (def donut-img cookie-img)

(def default-db
  {:page :home
   :ws-chan (atom nil)
   :stats {:count 1}
   :config {:title "Decide cookiewarrior"
            :duration 15
            :left  {:title "Cookies"
                    :img cookie-img
                    :count 10
                    :clicks 0}
            :right {:title "Donuts"
                    :img donut-img
                    :count 10
                    :clicks 0}}})
