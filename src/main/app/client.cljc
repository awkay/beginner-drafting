(ns app.client
  (:require
    [app.application :refer [SPA]]
    [app.ui.root :as root]
    [com.fulcrologic.fulcro.application :as app]
    [taoensso.timbre :as log]))

(defn ^:export refresh []
  (log/info "Hot code Remount")
  (app/mount! SPA root/Root "app"))

(defn ^:export init []
  (log/info "Application starting.")
  (app/mount! SPA root/Root "app"))
