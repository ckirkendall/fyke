(defproject fyke "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [reagent "0.6.0-alpha"]
                 [cljsjs/d3 "3.5.3-0"]
                 [stanford-talk "0.1.0"]
                 [net.mikera/vectorz-clj "0.44.0"]
                 [org.clojure/math.combinatorics "0.1.1"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [com.cognitect/transit-cljs "0.8.237"]]
  :plugins [[lein-cljsbuild "1.1.3"]]
  :cljsbuild
  {:builds [{:id "app"
             :source-paths ["src"]
             :compiler {:output-to "app.js"
                        :main fyke.core
                        :optimizations :whitespace
                        :pretty-print true}}]})
