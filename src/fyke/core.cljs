(ns fyke.core
  (:require-macros [fyke.model-builder :as mb])
  (:require [fyke.graph :as graph]
            [cognitect.transit :as transit]))


(def data (transit/read
            (transit/reader :json)
            (mb/inline-model "alice-wonderland.transit")))

(defn create-node [type word cnt idx]
  #js{:name word
      :index idx
      :radius (int (* (js/Math.pow cnt .4) 1.5))
      :class (name type)})

(defn add-node [data nodes [word sim type]]
  (if-let [node (or (some #(when (= (.-name %) word) %)
                          nodes))]
    (do
      (js/console.log "NODE_EXISTS:" node)
      node)
    (let [cnt (get-in data [:cnt type word])
          node (create-node type word cnt (count nodes))]
      (.push nodes node)
      node)))

(defn add-link [links sim source target]
  (let [link #js{:source source :target target :sim sim}]
    (.push links link)))

(defn get-child-words [data word type]
  (map #(conj % type)
    (take 10 (filter #(not= (first %) "illustration")
                    (get-in data [word type])))))


(defn add-child [data]
  (fn [force node]
    (let [nodes (.nodes force)
          links (.links force)
          word (.-name node)
          _ (js/console.log "WORD:" node)
          child-words (mapcat (partial get-child-words data word)
                              [:noun :adjective])]
      (doseq [[_ sim _ :as w] child-words]
        (let [w-node (add-node data nodes w)]
          (add-link links sim node w-node))))))

(defn distance-fn [link]
  (* 600 (- 1 (.-sim link))))

(defn init-graph []
  (let [cnt1 (get-in data [:cnt :noun "hatter"])
        cnt2 (get-in data [:cnt :noun "alice"])
        cnt3 (get-in data [:cnt :noun "queen"])
        nodes #js[(create-node :noun "hatter" cnt1 0)
                  (create-node :noun "alice" cnt2 1)
                  (create-node :noun "queen" cnt3 2)]
        links #js[]]
    (js/console.log (pr-str (:cnt data)))
    (graph/init (.-innerWidth js/window)
                (.-innerHeight js/window)
                nodes
                links
                (add-child data)
                distance-fn)))

(init-graph)
