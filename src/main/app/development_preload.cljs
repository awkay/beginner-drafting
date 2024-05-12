(ns app.development-preload
  (:require
    [taoensso.timbre :as log]))

(js/console.log "Turning logging to :debug (in app.development-preload)")
(log/set-level! :debug)

