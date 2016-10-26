(ns cookiewars.db)

(def img-url "http://i3.istockimg.com/file_thumbview_approve/93368057/5/stock-illustration-93368057-chocolate-chip-cookie.jpg")

(def default-db
  {:page :home
   :img img-url
   :ws-chan (atom nil)
   :battle {:title "Decide cookiewarrior"
            :duration 15
            :left  {:title "Cookies"
                    :count 10
                    :clicks 0}
            :right {:title "Candies"
                    :count 10
                    :clicks 0}}})
