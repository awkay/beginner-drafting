(ns app.todo
  (:require
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom])
    [app.apis.todo :as todo]                                ; IMPORTANT: Side-effects to make sure our resolvers and mutations are defined before we build the server
    [app.sample-servers.registry]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as target]
    [com.fulcrologic.rad.type-support.date-time :as dt]
    [app.application :refer [build-app render-app-element]]))

(defonce render-data? (atom false))
(defonce SPA (build-app render-data?))

(defsc Item [this {:item/keys [id label complete?]}]
  {:query [:item/id :item/label :item/complete?]
   :ident :item/id}
  (dom/div {:id (str "item" id)}
    (dom/input {:type "checkbox" :checked true #_(boolean complete?)})
    (dom/span (str label))))

(def ui-item (comp/factory Item {:keyfn :item/id}))

(defsc List [this {:list/keys [id title items]}]
  {:query [:list/id :list/title {:list/items (comp/get-query Item)}]
   :ident :list/id}
  (dom/div {:id (str "list" id)}
    (dom/h4 {} (str title))
    (dom/ul {}
      (mapv ui-item items))))

(def ui-list (comp/factory List {:keyfn :list/id}))

(defsc Root [this {:todo/keys [lists]}]
  {:query         [{:todo/lists (comp/get-query List)}]
   :initial-state {:todo/lists []}}
  (dom/div
    (mapv ui-list lists)))

(comment
  (render-app-element SPA "item4")
  (reset! render-data? false)
  (reset! render-data? true)
  (reset! (::app/state-atom SPA) {})
  (app/mount! SPA Root :k)
  (app/schedule-render! SPA {:force-root? true})
  (merge/merge-component! SPA Item {:item/id 1 :item/label "A"})
  (app/current-state SPA)
  (comp/transact! SPA [(todo/create-list {:list/title (str "List" (mod (long (/ (dt/now-ms) 100)) 10000))})])
  (comp/transact! SPA [(todo/append-item {:list/id 1
                                          :item/label (str "Item" (mod (long (/ (dt/now-ms) 100)) 10000))})])
  (df/load! SPA :all-lists List {:target [:todo/lists]})
  (df/load! SPA :todo/list List {:params {:list/id 11}
                                 :target (target/append-to [:todo/lists])})
  (app/schedule-render! SPA {:force-root? true})
  )
