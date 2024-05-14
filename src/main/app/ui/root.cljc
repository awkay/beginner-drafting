(ns app.ui.root
  (:require
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom])
    [app.ui.components :as ac]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.rpl.specter :as sp]
    [taipei-404.html :refer [html->hiccup]]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [taoensso.timbre :as log]))

(defsc Root [this props]
  {:query         [:todo]
   :initial-state {:todo "Start"}}
  (dom/div :#top {}
    (dom/div :#a "A")
    (ac/ui-placeholder {:w 100 :h 100 :label "X"})
    (dom/div :#b "B"
      (dom/span :#c "C"))
    (str "TODO" (:todo props))))

(comment
  (render-and-get Root {:todo "Foo"} "top")

  )
