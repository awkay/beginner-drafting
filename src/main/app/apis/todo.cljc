(ns app.apis.todo
  (:require
    [app.apis.database :as db]
    [app.sample-servers.registry :refer [build-eql-processor defmutation defresolver]]
    [clojure.set :as set]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as target]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.wsscode.pathom.connect :as pc]
    [datascript.core :as d]))

(defn load-list! [db id]
  (let [result (d/pull
                 (d/db db/conn)
                 [:db/id
                  :list/title
                  {:list/items [:db/id]}]
                 id)]
    (-> result
      (set/rename-keys {:db/id :list/id})
      (update :list/items (fn [i] (mapv #(set/rename-keys % {:db/id :item/id}) i))))))

(defresolver todo-list-resolver [env input]
  {::pc/output [{:todo/list [{:list/items [:item/id]}]}]}
  (let [{:list/keys [id]} (:query-params env)]
    {:todo/list (load-list! (d/db db/conn) id)}))

(m/defmutation create-list [{:list/keys [title]}]
  (remote [env]
    (-> env
      (m/with-target (target/append-to [:todo/lists]))
      (m/returning 'app.todo/TodoList))))

(m/defmutation set-complete [{:item/keys [id complete?]}]
  (action [{:keys [state]}]
    (swap! state assoc-in [:item/id id :item/complete?] (boolean complete?)))
  (remote [_] true))

(defmutation set-complete-server [_ {:item/keys [id complete?]}]
  {::pc/sym `set-complete}
  (println "Server toggle item")
  (d/transact db/conn [[:db/add id :item/complete? (boolean complete?)]])
  nil)

(defmutation create-list-server [_ {:list/keys [title]}]
  {::pc/sym    `create-list
   ::pc/output [:list/id]}
  (println "Create server list " title)
  {:list/id (db/create! {:list/title title})})

(defresolver todo-list-ident-resolver [env {:list/keys [id]}]
  {::pc/input  #{:list/id}
   ::pc/output [:list/id
                :list/title
                {:list/items [:item/id]}]}
  (load-list! (d/db db/conn) id))

(defresolver all-list-resolver [env {:list/keys [id]}]
  {::pc/output [{:all-lists [:list/id]}]}
  (let [db (d/db db/conn)]
    {:all-lists (mapv
                  (fn [[id]] {:list/id id})
                  (d/q '[:find ?id
                         :where
                         [?id :list/title]]
                    db))}))

(defresolver todo-item-resolver [env {:item/keys [id]}]
  {::pc/input  #{:item/id}
   ::pc/output [:item/label
                :item/complete?]}
  (println "RESOLVE ITEM DETAILS " id)
  (let [result (d/pull
                 (d/db db/conn)
                 [:db/id
                  :item/label
                  :item/complete?]
                 id)]
    (set/rename-keys result {:db/id :list/id})))

(m/defmutation append-item [{:list/keys [id]
                             :item/keys [label]}]
  (remote [env] (m/returning env 'app.todo/TodoList)))

(defmutation append-item-server [env {list-id    :list/id
                                      :item/keys [label] :as new-item}]
  {::pc/sym    `append-item
   ::pc/output [:list/id]}
  (println "Append item server " list-id label)
  (when (db/entity-exists? list-id)
    (let [item-id (db/create! {:item/label     label
                               :item/complete? false})]
      (d/transact db/conn [[:db/add list-id :list/items item-id]])
      {:list/id list-id})))

(comment
  (let [parser (build-eql-processor)]
    (defn run-eql! [tx] (parser {} tx)))

  (db/entity-exists? 1)
  (run-eql! `[(create-list {:list/title "Today's List"})])
  (run-eql! `[(append-item {:list/id    1
                            :item/label "Buy Milk"})])
  (run-eql! `[(append-item {:list/id    1
                            :item/label "Sell Cherries"})])
  (run-eql! `[{[:list/id 1] [:list/title
                             {:list/items [:item/id :item/label :item/complete?]}]}]))
