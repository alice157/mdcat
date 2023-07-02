(ns mdcat.generation
  (:require 
    [mdcat.git :as git]
    [mdcat.markdown :as md]
    [mdcat.xforms :as xforms])
  (:import
    java.util.Date))


(defn task
  [config output [action in out]]
  (case action
    :read-file (assoc-in output [:files out] (slurp (str (:cd config) in)))
    :output-file (spit (str (:cd config) out)
                       (get-in output [:files in]))
    :xform-file (update-in output [:files in] md/xform-string (comp xforms/unpack-list xforms/reverse-container))))


(defn generate
  [config]
  (let [nv-ctx {:cd (:cd config)}
        txn (nv/open-txn nv-ctx "main")]
    (reduce (partial task config) {} (:tasks config))
    (nv/close-txn nv-ctx txn (str "Generated at " (System/currentTimeMillis)))))


(comment
  (generate config))
