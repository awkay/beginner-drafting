(ns app.todo
  (:require
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom])
    [app.apis.todo :as todo]
    [app.sample-servers.registry]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as target]
    [com.fulcrologic.rad.type-support.date-time :as dt]
    [app.application :refer [build-app render-app-element click-on! txn-handler]]))

(defonce render-data? (atom false))
(defonce SPA (build-app render-data?))

(defsc Item [this {:item/keys [id label complete?]}]
  {:query [:item/id :item/label :item/complete?]
   :ident :item/id}
  (dom/div {:id (str "item" id)}
    (dom/input {:type    "checkbox"
                :id      (str "item" id "-checkbox")
                :onClick (txn-handler this [(todo/set-complete {:item/id id :item/complete? (not complete?)})])
                :checked (boolean complete?)})
    (dom/span (str label))))

(def ui-item (comp/factory Item {:keyfn :item/id}))

(defsc TodoList [this {:list/keys [id title items]}]
  {:query [:list/id :list/title {:list/items (comp/get-query Item)}]
   :ident :list/id}
  (dom/div {:id (str "list" id)}
    (dom/h4 {} (str title))
    (dom/ul {}
      (mapv ui-item items))))

(def ui-list (comp/factory TodoList {:keyfn :list/id}))

(defsc Root [this {:todo/keys [lists]}]
  {:query         [{:todo/lists (comp/get-query TodoList)}]
   :initial-state {:todo/lists []}}
  (dom/div
    (mapv ui-list lists)))

(defn numbered-name
  "Generates a string based on nm that has a number appended which
   will likely be different each time you call it."
  [nm]
  (str nm "-" (mod (long (/ (dt/now-ms) 100)) 10000)))

(comment
  (reset! render-data? false)
  (reset! render-data? true)

  (app/mount! SPA Root :ignored)
  (reset! (::app/state-atom SPA) {})

  (app/schedule-render! SPA {:force-root? true})

  (merge/merge-component! SPA Item {:item/id 99 :item/label "A"})

  (app/current-state SPA)

  (comp/transact! SPA [(todo/set-complete {:item/id 3 :item/complete? false})])
  (comp/transact! SPA [(todo/create-list {:list/title (numbered-name "List")})])
  (comp/transact! SPA [(todo/append-item {:list/id    1
                                          :item/label (numbered-name "Item")})])
  (df/load! SPA :all-lists TodoList {:target [:todo/lists]})
  (df/load! SPA :todo/list TodoList {:params {:list/id 11}
                                     :target (target/append-to [:todo/lists])})
  (app/schedule-render! SPA {:force-root? true})

  (render-app-element SPA "item3")

  (comp/transact! SPA (read-string "[(app.apis.todo/set-complete #:item{:id 4, :complete? false})]"))
  (click-on! SPA "item4-checkbox")
  )
