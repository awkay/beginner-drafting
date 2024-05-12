(ns app.application
  (:require
    [com.fulcrologic.fulcro.application :as app]))

(defonce SPA (app/fulcro-app {}))
