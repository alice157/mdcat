(ns mdcat.generation
  (:require 
    [mdcat.git :as git]
    [mdcat.markdown :as md]
    [mdcat.xforms :as xforms])
  (:import
    java.util.Date))


(set! *warn-on-reflection* true)


(defn task
  [config output [action in out]]
  (case action
    :read-file (assoc-in output [:files out] (slurp (str (:cd config) in)))
    :output-file (spit (str (:cd config) out)
                       (get-in output [:files in]))
    :xform-file (update-in output [:files in] md/xform-string xforms/reverse-top-level-lists)))


(defn generate
  [config]
  (let [git-ctx {:cd (:cd config)}
        txn (git/open-txn git-ctx "main")]
    (reduce (partial task config) {} (:tasks config))
    (git/close-txn git-ctx txn (str "Generated at " (System/currentTimeMillis)))))
