(ns app.form
  (:require
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom] :clj [com.fulcrologic.fulcro.dom-server :as dom])
    [app.application :refer [build-app]]
    [app.sample-servers.registry]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.mutations :as m]))

(defonce render-data? (atom false))
(defonce SPA (build-app render-data?))

(def person-form-status (fs/make-validator
                          (fn [form field]
                            (let [v (get form field "")]
                              (case field
                                (:person/last-name :person/first-name) (boolean (seq v))
                                true)))))

(defsc Person [this {:person/keys [id first-name last-name] :as props}]
  {:query       [:person/id
                 :person/first-name
                 :person/last-name
                 fs/form-config-join]
   :form-fields #{:person/first-name
                  :person/last-name}
   :ident       :person/id}
  (dom/div {:data-form-status (str (person-form-status props))}
    (dom/input {:value       (str first-name)
                :data-status (str (person-form-status props :person/first-name))})
    (dom/input {:value       (str last-name)
                :data-status (str (person-form-status props :person/last-name))})))

(def ui-person (comp/factory Person {:keyfn :person/id}))

(defsc Root [this {:root/keys [person]}]
  {:query         [{:root/person (comp/get-query Person)}]
   :initial-state {}}
  (dom/div
    (when person
      (ui-person person))))

(comment
  (reset! render-data? false)
  (reset! render-data? true)
  (reset! (::app/state-atom SPA) {})
  (app/mount! SPA Root :k)
  (app/schedule-render! SPA {:force-root? true})
  (merge/merge-component! SPA Person (fs/add-form-config Person {:person/id 1 :person/first-name ""}) :replace [:root/person])
  (comp/transact! SPA [(m/set-props {:person/first-name "Tom"})] {:ref [:person/id 1]})
  (comp/transact! SPA [(fs/mark-complete! {:entity-ident [:person/id 1]
                                           :field        :person/first-name})])
  (comp/transact! SPA [(m/set-props {:person/last-name ""})] {:ref [:person/id 1]})
  (comp/transact! SPA [(fs/mark-complete! {:entity-ident [:person/id 1]
                                           :field        :person/last-name})])
  (comp/transact! SPA [(fs/reset-form! {:form-ident [:person/id 1]})])
  (clojure.pprint/pprint (app/current-state SPA))
  (app/schedule-render! SPA {:force-root? true}))
