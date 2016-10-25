(ns user
  (:require [mount.core :as mount]
            [cookiewars.figwheel :refer [start-fw stop-fw cljs]]
            cookiewars.core))

(defn start []
  (mount/start-without #'cookiewars.core/repl-server))

(defn stop []
  (mount/stop-except #'cookiewars.core/repl-server))

(defn restart []
  (stop)
  (start))


