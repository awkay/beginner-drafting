(ns app.ui.root
  (:require
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li p h3 button b]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [taoensso.timbre :as log]))

(defsc Root [this props]
  {:query         [:todo]
   :initial-state {}}
  (dom/div "TODO"))
