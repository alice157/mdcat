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
  (if (pos? n)
    (str/join (apply str (repeat n "\n"))
              (map str/trim-newline strs))
    (str/join (map str/trim-newline strs))))


;; this indents every string, should we actually indent each line?
;; should see what parser does here and match
(defn indents
  [level]
  (apply str (repeat level INDENT)))


(defn tag-key
  [md & _]
  (if (string? md)
    ::string
    (first md)))


(def render (make-hierarchy))

(derive render :md/paragraph ::inline-container)
(derive render :md/bullet-list ::newline-container)
(derive render :md/document ::section-container)

(derive render :md/text ::string)
(derive render :md/soft-line-break ::string)

#_(ns-unmap *ns* 'render*)
(defmulti render* tag-key :hierarchy render)


(defmethod render* :default
  [md ctx]
  (throw (ex-info (str "Unknown tag: " (tag-key md)) ctx)))


(defmethod render* ::inline-container
  [md ctx]
  (->> (rest md)
       (map #(render* % ctx))
       (join-with-n-newlines 0)))


(defmethod render* ::newline-container
  [md ctx]
  (->> (rest md)
       (map #(render* % ctx))
       (join-with-n-newlines 1)))


(defmethod render* ::string
  [md _]
  (str (second md)))


(defmethod render* ::section-container
  [md ctx]
  (->> (rest md)
       (map #(render* % ctx))
       (join-with-n-newlines 2)))


(defmethod render* :md/document
  [md ctx]
  (-> (render* (assoc md 0 ::section-container) ctx)
      (str "\n")))


(defn bullet-list-item
  [s]
  (str BULLET s))


(defmethod render* :md/bullet-list-item
  [md ctx]
  (->> (into [(str (indents (:indent-level ctx))
                   (bullet-list-item (render* (second md) ctx)))]
             (map #(render* % (update ctx :indent-level inc)))
             (drop 2 md))
       (join-with-n-newlines 1)))


(defn render
  [md]
  (render* md {:indent-level 0}))
