(ns app.apis.form
  (:require
    [app.apis.database :as db]
    [app.sample-servers.registry :refer [build-eql-processor defmutation defresolver]]
    [clojure.set :as set]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as target]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.wsscode.pathom.connect :as pc]
    [datascript.core :as d]))

(defn load-person! [db id]
  (let [result (d/pull db [:db/id :person/first-name :person/last-name] id)]
    (println "Person found: " result)
    (set/rename-keys result {:db/id :person/id})))

(defresolver todo-list-ident-resolver [env {:person/keys [id]}]
  {::pc/input  #{:person/id}
   ::pc/output [:person/id
                :person/first-name
                :person/last-name]}
  (load-person! (d/db db/conn) id))

(m/defmutation save [params] (remote [_] true))

(defmutation save-server [env {:keys [delta]}]
  {::pc/sym `save}
  (println "SAVE: " delta)
  {})

(comment
  (let [parser (build-eql-processor)]
    (defn run-eql! [tx] (parser {} tx)))

  (d/transact! db/conn [{:person/id         "id"
                         :person/first-name "Bob"
                         :person/last-name  "Farth"}])

  (d/transact! db/conn [{:person/id         "id"
                         :person/first-name "Sally"
                         :person/last-name  "Park"}])

  (db/entity-exists? 1)
  (run-eql! `[{[:person/id 2] [:person/first-name]}])
  )
