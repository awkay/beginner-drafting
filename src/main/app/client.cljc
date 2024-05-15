(ns app.client
  (:require
    [app.todo :refer [Root SPA]]
    [com.fulcrologic.fulcro.application :as app]
    [taoensso.timbre :as log]))

(defn ^:export refresh []
  (log/info "Hot code Remount")
  (app/mount! SPA Root "app"))

(defn ^:export init []
  (log/info "Application starting.")
  (app/mount! SPA Root "app"))
