(ns app.apis.database
  (:require
    [datascript.core :as d]))

(defonce conn (d/create-conn {:list/items {:db/valueType   :db.type/ref
                                           :db/cardinality :db.cardinality/many}}))
(defn create!
  "Pass a map with the things you want to save. Returns the db id."
  [entity]
  (let [{{:strs [id]} :tempids} @(d/transact conn [(assoc entity :db/id "id")])]
    id))

(defn entity-exists? [id]
  (= id (:e (first (d/datoms (d/db conn) :eavt id)))))

