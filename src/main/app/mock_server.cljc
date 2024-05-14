(ns app.mock-server
  (:require
    [clojure.core.async :as async]
    [com.wsscode.pathom.connect :as pc]
    [com.wsscode.pathom.core :as p]))

(def resolvers (atom {}))

(pc/defresolver foo [env input]
  {::pc/output [:foo]}
  {:foo 42})

(swap! resolvers assoc `foo foo)

(def parser (p/async-parser {::p/env    {::p/reader [p/map-reader
                                                     pc/async-reader2]}
                             ::p/mutate pc/mutate-async
                             ::p/plugins
                             [(pc/connect-plugin
                                {::pc/register (vals @resolvers)})]}))

(defn run-eql! [tx]
  (parser {} tx))

(comment
  (async/<!! (parser {} [:foo])))
