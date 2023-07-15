(ns mdcat.main
  (:gen-class
    :main true)
  (:require
    [clojure.string :as str]
    [clojure.tools.cli :as cli]
    [mdcat.task :as task]
    [puget.printer :as puget]))


(set! *warn-on-reflection* true)


;; Forgive me
(def pipeline (atom []))


(defn- maybe-keywordify
  [s]
  (if (and (string? s) (str/starts-with? s ":"))
    (keyword (subs s 1))
    s))


(defn- split-args
  [s]
  (prn s)
  (cond
    (string? s)
    (map (comp maybe-keywordify str/trim) (str/split s #","))
    
    :else
    [nil]))


(defn- conj-pipeline-fn
  [k]
  (fn conj-pipeline
    [_ args]
    (swap! pipeline
           conj (into [k] (map maybe-keywordify (split-args args))))))


(def cli-options
  [["-h" "--help"]
   ["-o" "--opts" "prints parsed options"]
   ["-s" "--select FROM,[TO]" "selects a resource"
    :multi true
    :update-fn (conj-pipeline-fn :select)]
   ["-x" "--xform [[FROM]],XFORM_ID,[TO]" "transforms a resource"
    :multi true
    :update-fn (conj-pipeline-fn :xform)]
   ["-w" "--write [TO]" "writes a resource"
    :multi true
    :update-fn (conj-pipeline-fn :write)]
   ["-p" "--parse" "prints a parse tree and passes through"
    :multi true
    :update-fn (conj-pipeline-fn :parse)]

   ["-r" "--read [[FROM]]" "reads a file"
    :multi true
    :update-fn (conj-pipeline-fn :read)]
   ["-X" "--xform2 SELECTOR,XFORM" "super secret xform2"
    :multi true
    :update-fn (conj-pipeline-fn :xform2)]])


(defn usage
  [opts]
  (println (str "Usage:\n" (:summary opts))))


(defn errors
  [opts]
  (doseq [err (:errors opts)]
    (println err))
  (System/exit 1))


(defn reduce-pipeline
  [pipeline]
  (let [out (reduce task/reducer {} pipeline)]
    (if-not (:did-output? out)
      (apply (partial task/print-resource out) [nil])
      out)))


(defn parse-opts
  [args]
  (reset! pipeline [])
  (let [opts (cli/parse-opts args cli-options)
        pipeline @pipeline]
    (if (seq pipeline)
      (assoc opts :pipeline pipeline)
      opts)))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [opts (parse-opts args)]
    (when (get-in opts [:options :opts])
      (puget/cprint opts))
    (cond
      (seq (:errors opts))
      (errors opts)
  
      (get-in opts [:options :help])
      (usage opts)

      (seq (:pipeline opts))
      (do (println "Executing pipeline:")
          (puget/pprint (:pipeline opts))
          (reduce-pipeline (:pipeline opts)))


      :else
      (usage opts))))

(comment
