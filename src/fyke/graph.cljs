(ns fyke.graph
   (:require cljsjs.d3))

(def width 960)
(def height 960)
(def fill (.. js/d3 -scale category20))

(declare redraw)

(defn rescale []
  (let [trans (.. js/d3 -event -translate)
        scale (.. js/d3 -event -scale)]
    (-> js/d3
        (.select "#c-area")
        (.attr "transform"
               (str "translate(" trans ")"
                    " scale(" scale ")")))))

(defn tick [vis]
  (fn []
    (let [node (.selectAll vis ".node")
          link (.selectAll vis ".link")]
      (-> link
          (.attr "x1" #(.. % -source -x))
          (.attr "y1" #(.. % -source -y))
          (.attr "x2" #(.. % -target -x))
          (.attr "y2" #(.. % -target -y)))
      (-> node
          (.attr "x" #(.. % -x))
          (.attr "y" #(.. % -y))
          (.attr "transform"
                 #(str "translate(" (.-x %)
                       "," (.-y %) ")"))))))

(defn add-children [node-holder link-holder force child-fn]
  (fn [d]
    (child-fn force d)
    (redraw node-holder link-holder force child-fn)))


(defn redraw-links [vis force]
  (let [link (-> (.selectAll vis ".link")
                 (.data (.links force)))]
    (-> link
        (.enter)
        (.insert "line")
        (.attr "class" "link"))
    (-> link .exit .remove)))

(defn redraw-nodes [node-holder link-holder force child-fn]
  (let [node (-> (.selectAll node-holder ".node")
                 (.data (.nodes force)))
        nodeg (-> node
                  (.enter)
                  (.append "svg:g")
                  (.attr "class" "node"))]
    (-> nodeg
        (.append "svg:circle")
        (.attr "class" #(do
                          (js/console.log "NODE:" %)
                          (str "node-point " (.-class %))))
        (.attr "r" 5)
        (.on "dblclick" (add-children node-holder
                                      link-holder
                                      force
                                      child-fn))
        (.transition)
        (.duration 750)
        (.ease "elastic")
        (.attr "r" #(.-radius %)))
    (-> nodeg
        (.append "svg:text")
        (.attr "x" #(+ 4 (.-radius %)))
        (.attr "y" 4)
        (.attr "class" "node-text")
        (.text #(.-name %)))
    (-> node
        .exit
        .transition
        (.attr "r" 0)
        .remove)))


(defn redraw
  [node-holder link-holder force child-fn]
  (redraw-links link-holder force)
  (redraw-nodes node-holder link-holder force child-fn)
  (.start force))

(defn init [width height nodes links child-fn dis-fn]
  (let [outer (-> js/d3
                  (.select "#graph")
                  (.append "svg:svg")
                  (.attr "width" width)
                  (.attr "height" height)
                  (.attr "pointer-events" "all"))
        vis (-> outer
                (.call (.. js/d3 -behavior zoom (on "zoom" rescale)))
                (.on "dblclick.zoom" nil)
                (.append "svg:g")
                (.attr "id" "c-area"))
        rec (-> vis
                (.append "svg:rect")
                (.attr "width" width)
                (.attr "height" height)
                (.attr "fill" "white"))
        link-holder (-> vis
                        (.append "svg:g")
                        (.attr "class" "link-holder"))
        node-holder (-> vis
                        (.append "svg:g")
                        (.attr "class" "node-holder"))
        force (-> (.. js/d3 -layout force)
                  (.size #js[width height])
                  (.nodes nodes)
                  (.links links)
                  (.linkDistance dis-fn)
                  (.charge #(* -500 (.-radius %)))
                  (.on "tick" (tick vis)))]
    (js/console.log links)
    (js/console.log nodes)
    (redraw node-holder
            link-holder
            force
            child-fn)))
