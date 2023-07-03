(ns mdcat.task
  (:require
    [mdcat.markdown :as md]
    [mdcat.xforms :as xforms]
    [puget.printer :as puget]))


(set! *warn-on-reflection* true)

;; Task context tools

(defn last-resource
  [ctx k]
  (assoc ctx :last-resource k))


(defn read-resource
  [ctx resource]
  (let [resource (or resource (:last-resource ctx))]
    (if (keyword? resource)
      (get-in ctx [:resources resource])
      (slurp (str resource)))))


(defn output-resource
  [ctx input output]
  (let [s (read-resource ctx input)]
    (cond
      (nil? output)
      (println s)

      (keyword? output)
      (-> ctx
          (assoc-in [:resources output] s)
          (last-resource output))

      (string? output)
      (spit output s)

      :else
      (throw (ex-info "Cannot output to resource" {:output output})))))


;; Human-friendly building blocks

(defn select-resource
  ([ctx input as]
   (output-resource ctx input as))
  ([ctx input]
   (select-resource ctx input :selected)))


(defn xform-resource
  ([ctx input xform-id output]
   (let [s (read-resource ctx input)
         xformed (md/xform-string s (get xforms/xforms (keyword xform-id)))]
     (-> ctx
         (assoc-in [:resources output] xformed)
         (last-resource output))))
  ([ctx xform-id output]
   (xform-resource ctx :selected xform-id output))
  ([ctx xform-id]
   (xform-resource ctx :selected xform-id :xformed)))


(defn print-resource
  [ctx input]
  (println (str input ":\n"
                (read-resource ctx input) "\n"))
  (assoc ctx :did-output? true))


(defn write-resource
  [ctx output]
  (-> ctx
      (output-resource nil output)
      (assoc :did-output? true)))


(def tasks
  {:select select-resource
   :xform xform-resource
   :print print-resource
   :write write-resource})


(defn reducer
  [ctx [action & args]]
  (let [task-fn (partial (get tasks action) ctx)]
    (apply task-fn args)))
