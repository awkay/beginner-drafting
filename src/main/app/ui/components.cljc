(ns app.ui.components
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom])))

(defsc PlaceholderImage
  [this {:keys [w h label]}]
  (let [label (or label (str w "x" h))]
    (dom/svg {:width w :height h}
      (dom/rect {:width w :height h :style #js {:fill        "rgb(200,200,200)"
                                                :strokeWidth 2
                                                :stroke      "black"}})
      (dom/text {:textAnchor "middle" :x (/ w 2) :y (/ h 2)} label))))

(def ui-placeholder
  "Generates an SVG image placeholder of the given size and with the given label
  (defaults to showing 'w x h'.

  ```
  (ui-placeholder {:w 50 :h 50 :label \"avatar\"})
  ```
  "
  (comp/factory PlaceholderImage))
