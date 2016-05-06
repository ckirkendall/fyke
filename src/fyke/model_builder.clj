(ns fyke.model-builder
  (:require [clojure.core.matrix :as m]
            [clojure.core.matrix.linear :as ml]
            [stanford-talk.parser :as nlp]
            [clojure.set :as set]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.math.combinatorics :as math]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayOutputStream]))

(m/set-current-implementation :vectorz)

(def vec-size 10000)

(def pos-verb
  #{"VB" "VBZ" "VBD" "VBN" "VBG" "VBP"})

(def pos-noun
  #{"NN" "NNS" "NNP" "NNPS"})

(def pos-adj
  #{"JJ"})

(def pos-types
  (set/union pos-verb pos-noun pos-adj))

(def stop-words
  (into #{} (line-seq (io/reader (io/resource "stop_words.txt")))))


(defn add-type [{:keys [pos] :as word-data}]
  (assoc word-data
         :type
         (cond
           (pos-verb pos)
           :verb
           (pos-noun pos)
           :noun
           (pos-adj pos)
           :adjective
           :else :error)))

(defn log-cnt [val]
  (println "CNT:" (count val))
  val)

(defn extract-text [text]
  (->> text
       java.io.StringReader.
       java.io.BufferedReader.
       line-seq
       (drop-while #(not (.startsWith % "***")))
       rest
       (take-while #(not (.startsWith % "***")))
       (apply str)))

(defn extract-ctx-data [text]
  (->> text
       extract-text
       nlp/process-text
       :token-data
       (remove #(or (stop-words (:token %))
                    (<= (count (:token %)) 2)
                    (<= (count (:lemma %)) 2)))
       (filter #(pos-types (:pos %)))
       (map add-type)
       (partition-by :sent-num)
       (partition 20)
       (map #(apply concat %))))



(defn add-ctx [model ctx]
  (reduce
   (fn [model [type word idx]]
     (-> model
         (update-in [:cnt type word] (fnil inc 0))
         (update-in [:vec type word]
                    #((fnil m/mset! (m/new-sparse-array :vectorz [1 10000])) % 0 idx 1))))
   model
   (for [idx (take 10 (repeatedly #(rand-int 10000)))
         {:keys [type lemma]} ctx]
     [type (str/lower-case lemma) idx])))


(defn ctx-data->model [ctx-data]
  (reduce add-ctx {} ctx-data))

(defn top-words [model type]
  (->> (get-in model [:cnt type])
       (sort-by second)
       reverse
       (map first)
       (take 100)))

(defn cosine-sim
  "Cosine similarity between two hypervectors"
  [v1 v2]
  (let [norm1 (ml/norm v1)
        norm2 (ml/norm v2)]
    (when (and (pos? norm1) (pos? norm2))
      (/ (m/dot v1 v2) (* norm1 norm2)))))


(defn extract-similarities [model words]
  (let [comb (math/combinations words 2)]
    (reduce (fn [sim-model [[t1 w1] [t2 w2]]]
              (let [w1-vec (m/slice (get-in model [:vec t1 w1]) 0)
                    w2-vec (m/slice (get-in model [:vec t2 w2]) 0)
                    sim (cosine-sim w1-vec w2-vec)]
                (if (> sim 0.45)
                  (-> sim-model
                      (assoc-in [w1 t2 w2] sim)
                      (assoc-in [w2 t1 w1] sim))
                  sim-model)))
            (select-keys model [:cnt])
            comb)))

(defn sort-word-links [sim-model]
  (into (select-keys sim-model [:cnt])
        (map (fn [[word sims]]
               [word {:noun (reverse (sort-by second (:noun sims)))
                      :verb (reverse (sort-by second (:verb sims)))
                      :adjective (reverse (sort-by second (:adjective sims)))}])
          (dissoc sim-model :cnt))))


(defn write-out-model [sim-model out-file-name]
  (with-open [out (io/output-stream out-file-name)]
    (let [writer (transit/writer out :json)]
      (transit/write writer sim-model))))


(defn load-model [model-file-name]
     (with-open [in (io/input-stream
                     (io/resource model-file-name))]
       (let [reader (transit/reader in :json)]
         (transit/read reader))))


(defmacro inline-model [model-file-name]
  (let [model (load-model model-file-name)
           out (ByteArrayOutputStream. 4096)
           writer (transit/writer out :json)]
       (transit/write writer model)
       (.toString out)))

(comment
  (def tmp
    (let [text (slurp (io/resource "alices_adventures_in_wonderland.txt"))
          text-short (apply str (take 5000 text))
          model (ctx-data->model (extract-ctx-data text))
          nouns (map (partial vector :noun) (top-words model :noun))
          verbs (map (partial vector :verb) (top-words model :verb))
          adjs (map (partial vector :adjective) (top-words model :adjective))
          words (concat nouns verbs adjs)
          sim-model (extract-similarities model words)]
      (sort-word-links sim-model))))
