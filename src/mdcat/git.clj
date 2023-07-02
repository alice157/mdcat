(ns mdcat.git
  (:require
    [clojure.java.shell :as sh]
    [clojure.string :as str]))


(defn git
  [ctx & args]
  (apply sh/sh (concat ["/usr/bin/env" "git"] args [:dir (:cd ctx)])))


(defn add-files
  [ctx]
  (git ctx "add" "."))


(defn commit
  [ctx message]
  (git ctx "commit" "-m" message))


(defn checkout
  [ctx ref]
  (git ctx "checkout" ref))


(defn branch
  [ctx branch-name start-point]
  (git ctx "branch" branch-name start-point))


(defn dirty?
  [ctx]
  (not (str/blank? (:out (git ctx "status" "--porcelain")))))


(defn gen-branch-name
  []
  (str "txn-" (System/currentTimeMillis)))


(defn branch-name
  [ctx]
  (str/trim (:out (git ctx "branch" "--show-current"))))


(defn assert-clean!
  [ctx]
  (when (dirty? ctx)
    (throw (ex-info "Git context is dirty" ctx))))


(defn branch-off-ref
  [ctx ref]
  (assert-clean! ctx)
  (let [branch-name (branch-name ctx)
        new-branch-name (gen-branch-name)]
    (branch ctx new-branch-name ref)
    (checkout ctx new-branch-name)
    {:original-branch-name branch-name}))


(defn merge-branch
  [ctx from into]
  (assert-clean! ctx)
  (checkout ctx into)
  (git ctx "merge" from))


(defn commit-all
  [ctx message]
  (add-files ctx)
  (commit ctx message))

(commit-all context "test")


(defn open-txn
  [ctx ref]
  (when (dirty? ctx)
    (commit-all ctx "Preparing for transaction"))
  (branch-off-ref ctx ref))


(defn close-txn
  [ctx txn message]
  (commit-all ctx message)
  (merge-branch ctx (branch-name ctx) (:original-branch-name txn)))


(defn abort-txn
  [ctx txn message]
  (add-files ctx)
  (commit ctx message)
  (git checkout (:original-branch-name txn)))
