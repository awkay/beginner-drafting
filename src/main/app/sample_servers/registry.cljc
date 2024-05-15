(ns app.sample-servers.registry
  #?(:cljs (:require-macros app.sample-servers.registry))
  (:require
    [com.fulcrologic.fulcro.algorithms.tx-processing :as txn]
    [com.wsscode.pathom.connect :as pc]
    [com.wsscode.pathom.core :as p]
    [edn-query-language.core :as eql]
    [taoensso.encore :as enc]
    [taoensso.timbre :as log]))

(def query-params-to-env-plugin
  "Adds top-level load params to env, so nested parsing layers can see them."
  {::p/wrap-parser
   (fn [parser]
     (fn [env tx]
       (let [children     (-> tx eql/query->ast :children)
             query-params (reduce
                            (fn [qps {:keys [type params] :as x}]
                              (cond-> qps
                                (and (not= :call type) (seq params)) (merge params)))
                            {}
                            children)
             env          (assoc env :query-params query-params)]
         (parser env tx))))})

(def resolvers (atom {}))

(defn register! [k resolver]
  (swap! resolvers assoc k resolver))

#?(:clj
   (defmacro defresolver [sym arglist config & body]
     (let [helper-name (symbol (str (name sym) "-helper"))]
       `(do
          (defn ~helper-name ~arglist
            ~@body)
          (pc/defresolver ~sym [env# params#]
            ~config
            (~helper-name env# params#))
          (register! (quote ~sym) ~sym)))))

#?(:clj
   (defmacro defmutation [sym arglist config & body]
     (let [helper-name (symbol (str (name sym) "-helper"))]
       `(do
          (defn ~helper-name ~arglist
            ~@body)
          (pc/defmutation ~sym [env# params#]
            ~config
            (~helper-name env# params#))
          (register! (quote ~sym) ~sym)))))

(defn build-eql-processor []
  (p/parser {::p/env     {::p/reader [p/map-reader p/ident-join-reader pc/reader2]}
             ::p/mutate  pc/mutate
             ::p/plugins [(pc/connect-plugin {::pc/register (vals @resolvers)})
                          (p/post-process-parser-plugin p/elide-not-found)
                          query-params-to-env-plugin]}))

(defn mock-http-server
  "Create a remote that mocks a Fulcro remote server."
  []
  (let [parser   (build-eql-processor)
        run-eql! (fn [tx] (parser {} tx))]
    {:transmit! (fn transmit! [{:keys [active-requests]} {:keys [::txn/ast ::txn/result-handler ::txn/update-handler] :as send-node}]
                  (let [edn           (eql/ast->query ast)
                        ok-handler    (fn [result]
                                        (enc/catching
                                          (result-handler (select-keys result #{:transaction :status-code :body :status-text}))
                                          e
                                          (log/error e "Result handler failed with an exception. See https://book.fulcrologic.com/#err-msr-res-handler-exc")))
                        error-handler (fn [error-result]
                                        (enc/catching
                                          (result-handler (merge {:status-code 500} (select-keys error-result #{:transaction :status-code :body :status-text})))
                                          e
                                          (log/error e "Error handler failed with an exception. See https://book.fulcrologic.com/#err-msr-err-handler-exc")))]
                    (enc/catching
                      (let [result (run-eql! edn)]
                        (ok-handler {:transaction edn :status-code 200 :body result}))
                      e
                      (do
                        (log/error e "Server error")
                        (error-handler {:transaction edn :status-code 500})))))
     :abort!    (fn abort! [this id])}))
