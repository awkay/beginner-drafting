(ns app.form
  (:require
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom] :clj [com.fulcrologic.fulcro.dom-server :as dom])
    [app.application :refer [build-app txn-handler click-on!]]
    [app.apis.form :as api.form]
    [app.sample-servers.registry]
    [taoensso.encore :as enc]
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.data-fetch :as df]
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
                :data-status (str (person-form-status props :person/last-name))})
    (dom/button {:id      "save"
                 :onClick #?(:clj  (let [delta (fs/dirty-fields props true)]
                                     (pr-str [(api.form/save {:delta delta})]))
                             :cljs (fn []
                                     (let [delta (fs/dirty-fields this true)]
                                       (comp/transact! this [(api.form/save delta)]))))}
      "Save")))

(def ui-person (comp/factory Person {:keyfn :person/id}))

(defsc Root [this {:root/keys [person]}]
  {:query         [::app/active-remotes
                   {:root/person (comp/get-query Person)}]
   :initial-state {}}
  (dom/div
    (when person
      (ui-person person))))

(comment
  ;; 1. Anything you put in the config map of a component is something you can easily read from it:
  (comp/component-options Person)
  ;; 2. So, we can co-locate important information on the component. For example, for a form, we can
  ;; indicate which things in the query are "form fields" as opposed to information like "is the form
  ;; loading?" or something.
  (:form-fields (comp/component-options Person))
  ;; which of course we can make little helper functions for
  (fs/get-form-fields Person)

  (df/load! SPA [:person/id 1] Person {:target      [:root/person]
                                       :post-action (fn [{:keys [state]}]
                                                      (swap! state fs/add-form-config* Person [:person/id 1]
                                                        {:destructive? true}))})


  (reset! render-data? false)
  (reset! render-data? true)
  (reset! (::app/state-atom SPA) {})
  ;; Re-mounting root should refresh the queries used by Fulcro
  (app/mount! SPA Root :k)
  (app/schedule-render! SPA {:force-root? true})
  (merge/merge-component! SPA Person (fs/add-form-config Person {:person/id 1 :person/first-name ""}) :replace [:root/person])
  (comp/transact! SPA [(m/set-props {:person/first-name "Tom"})] {:ref [:person/id 1]})
  (comp/transact! SPA [(fs/mark-complete! {:entity-ident [:person/id 1]
                                           :field        :person/first-name})])
  (comp/transact! SPA [(m/set-props {:person/last-name "New"})] {:ref [:person/id 1]})
  (comp/transact! SPA [(fs/mark-complete! {:entity-ident [:person/id 1]
                                           :field        :person/last-name})])
  (comp/transact! SPA [(fs/reset-form! {:form-ident [:person/id 1]})])

  (click-on! SPA "save")
  (pr-str "\"a\"")
  (read-string (str/replace "[(app.apis.form/save {[:person/id 1] #:person{:last-name {:before &quot;Farth&quot;, :after &quot;New&quot;}}})]"
     "&quot;" "\""))

  (clojure.pprint/pprint (app/current-state SPA))
  (app/schedule-render! SPA {:force-root? true}))
