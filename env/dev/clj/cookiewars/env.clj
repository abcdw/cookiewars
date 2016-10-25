(ns cookiewars.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [cookiewars.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[cookiewars started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[cookiewars has shut down successfully]=-"))
   :middleware wrap-dev})
