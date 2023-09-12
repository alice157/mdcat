(ns mdcat.util
  (:require
    [clojure.java.io :as io]
    [clojure.spec.alpha :as s])
  (:import
    java.io.File))


(set! *warn-on-reflection* true)


(defn nested-update
  [v f]
  (let [v' (f v)]
    (cond
      v'
      v'

      (or (vector? v) (list? v) (map? v))
      (into (empty v)
            (map #(nested-update % f))
            v)

      :else
      v)))


;; https://stackoverflow.com/a/29588071
(let [homedir (io/file (System/getProperty "user.home"))
      usersdir (.getParent homedir)]
  (defn home
    "With no arguments, returns the current value of the `user.home` system
     property. If a `user` is passed, returns that user's home directory. It
     is naively assumed to be a directory with the same name as the `user`
     located relative to the parent of the current value of `user.home`."
    ([] homedir)
    ([user] (if (empty? user) homedir (io/file usersdir user)))))


;; https://stackoverflow.com/a/29588071
(defn expand-home
  "If `path` begins with a tilde (`~`), expand the tilde to the value
  of the `user.home` system property. If the `path` begins with a
  tilde immediately followed by some characters, they are assumed to
  be a username. This is expanded to the path to that user's home
  directory. This is (naively) assumed to be a directory with the same
  name as the user relative to the parent of the current value of
  `user.home`."
  [path]
  (let [path (str path)]
    (if (.startsWith path "~")
      (let [sep (.indexOf path File/separator)]
        (if (neg? sep)
          (home (subs path 1))
          (io/file (home (subs path 1 sep)) (subs path (inc sep)))))
      path)))


(def ^{:arglists '([] [coll] [coll x] [coll x & xs])
       :doc "Like `clojure.core/conj`, but defaults to a vector instead of a list."}
  conjv
  (fn conj
    [& args]
    (apply (fnil conj []) args)))


(defn safe
  "Returns a fn that wraps `f` and returns nil if an `Exception` is thrown."
  [f]
  (fn safe-fn
    [& args]
    (try
      (apply f args)
      (catch Exception _
        nil))))


(defmacro tapped
  [fn-sym]
  (let [fn-name (str "tapped-" (name fn-sym))]
    `(fn ~(symbol fn-name)
       [& args#]
       (let [res# (apply ~fn-sym args#)]
         (tap> [~(keyword fn-name) args# res#])
         res#))))
