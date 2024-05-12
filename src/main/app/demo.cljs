(ns app.demo
  (:require
    [com.fulcrologic.fulcro.dom :as dom]
    ["react-dom" :as react.dom]
    [clojure.pprint :refer [pprint]]))

(defonce state
  (atom {:team-a-picks [nil nil nil nil nil]
         :team-b-picks [nil nil nil nil nil]
         :team-a-bans  [nil nil nil nil nil]
         :team-b-bans  [nil nil nil nil nil]}))

;; Side effect at the start and load the champion data file
(defonce loaded-champions
  (do
    (->
      (js/fetch "https://ddragon.leagueoflegends.com/cdn/12.4.1/data/en_US/champion.json")
      (.then (fn [response-promise]
               (-> (.json response-promise)
                 (.then
                   (fn [json-promise]
                     (let [data   (js->clj json-promise :keywordize-keys true)
                           champs (vals (get data :data))]
                       (swap! state assoc :champions (zipmap (map :name champs) champs)))))))))
    true))

(defn ui-app-state [current-state]
  (dom/div {:style {:color "green"}}
    (dom/hr)
    (dom/h5 "Your app state is currently:")
    (dom/pre
      (with-out-str
        (pprint current-state)))
    (dom/hr)))

(defn mark-champion [old name]
  (assoc-in old [:champions name :used?] true))

(defn clear-champion [old name]
  (assoc-in old [:champions name :used?] false))

(defn champion-image-url [nm]
  (let [filename (get-in @state [:champions nm :image :full])]
    (str "https://ddragon.leagueoflegends.com/cdn/12.4.1/img/champion/" filename)))

(defn ui-header-layout [col1 col2 col3]
  (dom/div
    :.flex
    (dom/div :.w-48.flex.items-center.justify-right.text-center.p-8.mr-20 col1)
    (dom/div :.flex-1.flex.items-center.flex-wrap.whitespace-nowrap.justify-center.text-center.p-10 col2)
    (dom/div :.w-48.flex.items-center.justify-center.text-center.p-8.mr-20 col3)))

(defn ui-body-layout [col1 col2 col3]
  (dom/div
    :.flex {:style {:width          "100%"
                    :justifyContent "space-between"
                    :overflow       "wrap"}}
    (dom/div :.flex.items-top.ml-8 col1)
    (dom/div :.flex.mt-28.flex-grow-1 col2)
    (dom/div :.flex.items-top.mr-8.ml-1 col3)))

(defn ui-ban-list [bans]
  (dom/div
    :.flex {}
    (map-indexed
      (fn [idx champion]
        (dom/div :.w-10.h-10.bg-gray-200.m-2.mt-10.rounded-lg {:style {:overflow "hidden"}
                                                               :key   idx}
          (dom/img {:src   "https://via.placeholder.com/48"
                    :style {:width "100%" :height "100%"}})))
      bans)))

(defn clear-champion-and-selection [old-state champion team-list-name idx]
  (-> (clear-champion old-state champion)
    (assoc-in [team-list-name idx] nil)))

(defn pick-champion [old-state team-list-name slot-index champion-name]
  (-> old-state
    (assoc-in [team-list-name slot-index] champion-name)
    (assoc :selected nil)
    (assoc-in [:champions champion-name :used?] true)))

(defn ui-pick-list [team-list-name]
  (let [picks (get @state team-list-name)]
    (dom/div :.flex-col.w-40 {}
      (map-indexed
        (fn [idx champion]
          (dom/div :.w-20.h-20.bg-gray-200.m-2.mt-10.rounded-lg
            {:key   idx
             :style {:overflow "hidden"
                     :opacity  (if (get-in @state [champion :used]) 0.5 1)}}
            (if champion
              (dom/img {:src     (champion-image-url champion)
                        :onClick (fn []
                                   (swap! state clear-champion-and-selection champion team-list-name idx))
                        :width   100})
              (dom/img {:src     "https://via.placeholder.com/100"
                        :onClick (fn []
                                   (let [choice (get @state :selected)]
                                     (swap! state pick-champion team-list-name idx choice)))
                        :width   100}))))
        picks))))

(def scroll-styles
  (dom/style {} "
    ::-webkit-scrollbar {
      width: 8px;
    }
    ::-webkit-scrollbar-track {
      background-color: #f1f1f1;
    }
    ::-webkit-scrollbar-thumb {
      background-color: #888;
    }
    ::-webkit-scrollbar-thumb:hover {
      background-color: #555;
    }"))

(defn ui-champion-list [list]
  (let [currently-selected (get @state :selected)]
    (dom/div
      {:style {:maxHeight "calc(58vh)",                     ;; Adjust the max-height as needed
               :overflow  "auto"}}
      (dom/div scroll-styles)
      (dom/div
        :.flex.flex-row.flex-wrap.items-left.justify-center.overflow-x-auto
        (map
          (fn [champion]
            (let [nm        (get champion :name)
                  used?     (get-in @state [:champions nm :used?])
                  opacity   (if used? 0.3 1.0)
                  selected? (= nm currently-selected)]
              (dom/div :.w-16.h-16.m-4
                {:key     nm                                ;if you map over elements = warning, the top level element of that map needs a key to work efficiently
                 :onClick (fn [] (swap! state assoc :selected nm))
                 :style   {:border       (if selected? "2px solid white")
                           :borderRadius "30%"}}
                (dom/img {:src   (champion-image-url nm)
                          :style {:width        "100%" :height "100%"
                                  :borderRadius "30%"
                                  :opacity      opacity}}))))
          list)))))


(defn Root []
  (let [current-state @state
        all           (vals (get @state :champions))
        champions     (sort-by :name all)
        selection     (get @state :selected)]
    (dom/div
      (dom/div {}
        (ui-header-layout
          (dom/div
            (dom/div "Team A")
            (ui-ban-list (get @state :team-a-bans)))
          (dom/div "")
          (dom/div
            (dom/div "Team B")
            (ui-ban-list (get @state :team-b-bans))))
        (ui-body-layout
          (dom/div {}
            (dom/div "Team A Picks")
            (ui-pick-list :team-a-picks))
          (ui-champion-list champions)
          (dom/div {}
            (dom/div "Team B Picks")
            (ui-pick-list :team-b-picks)))
        (dom/button {:onClick (fn [] (swap! state update :show-state? not))} "Toggle State View")
        (when (:show-state? @state)
          (ui-app-state current-state))))))

;; REACT GLUE BELOW THIS LINE

(defn factory [cls]
  (fn [props]
    (dom/create-element cls #js {:props props})))

(def ui-root (factory Root))

(defn refresh []
  (let [dom-node (js/document.getElementById "app")]
    (react.dom/render (ui-root @state) dom-node)))

(defn init []
  (add-watch state :render (fn [_ _ _ _] (refresh)))
  (refresh))

