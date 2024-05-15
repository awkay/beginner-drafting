(ns app.application
  (:require
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom])
    [clojure.pprint :refer [pprint]]
    [app.sample-servers.registry :refer [mock-http-server]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.rpl.specter :as sp]
    [taoensso.timbre :as log]
    [taipei-404.html :refer [html->hiccup]]))

(defn hiccup-element-by-id [hiccup id]
  (sp/select-first (sp/walker (fn [n]
                                (and
                                  (vector? n)
                                  (map? (second n))
                                  (= id (:id (second n))))))
    hiccup))

(defn element->hiccup [element]
  #?(:clj
     (sp/transform
       (sp/walker map?)
       (fn [m] (dissoc m :data-reactroot :data-reactid :data-react-checksum))
       (first (html->hiccup (dom/render-to-str element))))))

(defn render-element [element]
  (pprint (element->hiccup element)))

(defn render-app-element [{::app/keys [state-atom runtime-atom]} id]
  (let [state-map @state-atom
        {::app/keys [root-class root-factory]} @runtime-atom
        query     (comp/get-query root-class state-map)
        tree      (fdn/db->tree query state-map state-map)]
    (-> (root-factory tree)
      (element->hiccup)
      (hiccup-element-by-id id))))

(defn click-on! [app id]
  #?(:clj
     (let [[_ {:keys [onClick]}] (render-app-element app id)
           txn (when (string? onClick) (some-> onClick read-string))]
       (comp/transact! app txn))))

(defn build-app
  ([] (build-app (volatile! true)))
  ([render-data-atom?]
   (let [last-state (volatile! {})]
     #?(:cljs
        (app/fulcro-app {:remotes {:remote (mock-http-server)}})
        :clj
        (letfn [(render [{::app/keys [runtime-atom state-atom] :as app} {:keys [force-root?]}]
                  (let [state-map @state-atom]
                    (when (or force-root? (not= state-map @last-state))
                      (let [{::app/keys [root-class root-factory]} @runtime-atom
                            query (comp/get-query root-class state-map)
                            tree  (fdn/db->tree query state-map state-map)]
                        (vreset! last-state state-map)
                        (if @render-data-atom?
                          (pprint tree)
                          (render-element (root-factory tree)))))))]
          (app/fulcro-app
            {:optimized-render! render
             :remotes           {:remote (mock-http-server)}}))))))
