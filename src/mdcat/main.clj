(ns mdcat.main
  (:gen-class
    :main true)
  (:require
    [clojure.edn :as edn]
    [clojure.java.shell :as sh]
    [clojure.tools.cli :as cli]
    [com.rpl.specter :as sp]
    [mdcat.markdown :as md]
    [mdcat.markdown.render :as mdr]
    [mdcat.selector :as sel]
    [puget.printer :as puget]))


(set! *warn-on-reflection* true)


(def cli-options2
  [["-h" "--help"]
   ["-o" "--opts" "prints parsed options"]
   ["-s" "--select SELECTOR"]
   ["-x" "--xform COMMAND"]
   ["-t" "--text" "output as text"]])


(defn usage
  [opts]
  (println (str "Usage:\n" (:summary opts))))



(defn run-command
  [command x]
  (edn/read-string (:out (sh/sh command :in (pr-str x)))))



(defn -main
  [& args]
  (let [opts (cli/parse-opts args cli-options2)]
    (when (get-in opts [:options :opts])
      (puget/cprint opts))
    (let [md (md/parse (slurp (first (:arguments opts))))
          selector (get-in opts [:options :select])
          xform (get-in opts [:options :xform])
          md' (cond
                xform
                (sp/transform (sel/selector (or selector "document"))
                              (partial run-command xform)
                              md)

                selector
                (sp/select (sel/selector selector) md)

                :else
                md)]
      (if (get-in opts [:options :text])
        (print (mdr/render md'))
        (puget/cprint md'))
      (flush)
      (System/exit 0))))
