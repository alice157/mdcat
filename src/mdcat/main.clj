(ns mdcat.main
  (:gen-class)
  (:require
    [clojure.tools.cli :as cli]
    [mdcat.generation :as gen]
    [mdcat.task :as task]
    [puget.printer :as puget]))


;; Forgive me
(def ^:dynamic *pipeline* nil)


(defn- conj-pipeline-fn
  [k]
  (fn conj-pipeline
    [_ x]
    (swap! *pipeline* conj [k x])))


(def cli-options
  [["-h" "--help"]
   ["-s" "--select NAME" "selects a resource"
    :multi true
    :update-fn (conj-pipeline-fn :select)]
   ["-x" "--xform XFORM" "transforms a resource"
    :multi true
    :update-fn (conj-pipeline-fn :xform)]
   ["-w" "--write NAME" "writes a resource"
    :multi true
    :update-fn (conj-pipeline-fn :write)]])


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
      (task/reducer out [:print nil])
      out)))


(defn parse-opts
  [args]
  (with-bindings {#'*pipeline* (atom [])}
    (let [opts (cli/parse-opts args cli-options)
          pipeline @*pipeline*]
      (if (seq pipeline)
        (assoc opts :pipeline pipeline)
        opts))))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [opts (parse-opts args)]
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
)
