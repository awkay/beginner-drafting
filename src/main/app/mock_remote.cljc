(ns app.mock-remote
  (:require
    [clojure.core.async :as async]
    [com.fulcrologic.fulcro.algorithms.tx-processing :as txn]
    [edn-query-language.core :as eql]
    [taoensso.encore :as enc]
    [taoensso.timbre :as log]))

(defn mock-http-server
  "Create a remote that mocks a Fulcro remote server.

  :parser - A function `(fn [eql-query] async-channel)` that returns a core async channel with the result for the
  given eql-query."
  [{:keys [parser] :as options}]
  (merge options
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
                      (async/go
                        (let [result (async/<! (parser edn))]
                          (ok-handler {:transaction edn :status-code 200 :body result})))
                      e
                      (error-handler {:transaction edn :status-code 500}))))
     :abort!    (fn abort! [this id])}))
