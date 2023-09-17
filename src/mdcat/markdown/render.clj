(ns mdcat.markdown.render
  (:require
    [clojure.string :as str]))


(def ^:const INDENT "  ")
(def ^:const BULLET "- ")


;; todo: compare to registry of known tags, throw if bad input
;; this could just be done by throwing from :default - but this
;; could result in spreading around tag validation
;; something like `(tag-key [:md/text "foo"]) -> :md/text` could be used everywhere
;; we need to pull out a tag. in the meantime, throwing from default isn't terrible

;; maybe reuse md/tag, extend it to vectors, and push the :md/unknown logic down
;; to the place where it's needed.
;; feels more clojure-y...


(defn join-with-n-newlines
  [n strs]
  (str/join (apply str (repeat n "\n"))
            (map str/trim-newline strs)))


;; this indents every string, should we actually indent each line?
;; should see what parser does here and match
(defn indent-str
  [level]
  (apply str (repeat level INDENT)))


(defn tag-key
  [md]
  (first md))


#_(ns-unmap *ns* 'render*)
(defmulti render* tag-key)


(defmethod render* :default
  [md ctx]
  (throw (ex-info (str "Unknown tag: " (tag-key md)) ctx)))


(defmethod render* :md/text
  [md _]
  (str (second md)))


(defmethod render* :md/bullet-list-item
  [md ctx]
  (let [])
  (join-with-n-newlines ))


(defn render
  [md]
  (render* md {:indent-level 0}))
