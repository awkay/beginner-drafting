(ns app.sample-test
  (:require
    [fulcro-spec.core :refer [specification assertions =>]]))

(specification "Foo"
  (assertions
    "Does work"
    true => true))
