(ns app.application
  (:require
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom])
    [app.mock-remote :refer [mock-http-server]]
    [app.mock-server :refer [run-eql!]]
    [app.ui.root :refer [Root]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp]
    [com.rpl.specter :as sp]
    [taipei-404.html :refer [html->hiccup]]))

(defn hiccup-element-by-id [hiccup id]
  (sp/select (sp/walker (fn [n]
                          (and
                            (vector? n)
                            (map? (second n))
                            (= id (:id (second n))))))
    hiccup))

(defn render-and-get [Component props id]
  (let [f             (comp/factory Component)
        dom-as-hiccup (render-root (f props))]
    (hiccup-element-by-id dom-as-hiccup id)))

(defn render-root [element]
  (html->hiccup (dom/render-to-str elements)))

(defn render [app _]

  )

(defonce SPA (app/fulcro-app
               {:optimized-render! render
                :render-root       render-root
                :remotes           {:remote (mock-http-server {:parser run-eql!})}}))

(comment

  (app/initialize-state! SPA Root)

  (app/current-state SPA)

  )
