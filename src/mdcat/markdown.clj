(ns mdcat.markdown
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.spec.alpha :as s]
    [mdcat.markdown.render :as mdr])
  (:import
    (com.vladsch.flexmark.ast
      BulletList
      BulletListItem
      Emphasis
      Heading
      Paragraph
      SoftLineBreak
      Text)
    com.vladsch.flexmark.parser.Parser
    (com.vladsch.flexmark.util.ast
      Document
      Node)
    (com.vladsch.flexmark.util.data
      MutableDataSet)))


(set! *warn-on-reflection* true)


(defprotocol Tag

  (tag [this]))


;; TODO: handle syms with namespaces
(defmacro deftag
  [class container?]
  (let [class-str (csk/->kebab-case-string class)
        predicate-sym (symbol (str class-str "?"))
        tag-kw (keyword "md" class-str)
        node-spec-kw (keyword "md" (str class-str "-node"))]
    `(do
       (extend-type ~class
         Tag
         (tag [_#] ~tag-kw))

       (derive ~tag-kw ~(if container? :md/container :md/leaf))

       (s/def ~node-spec-kw
         (s/and vector?
                #(= ~tag-kw (first %))))

       (defn ~predicate-sym
         [md#]
         (s/valid? ~node-spec-kw md#)))))


(deftag Text false)
(deftag Emphasis false)
(deftag SoftLineBreak false)

(deftag Heading true)
(deftag BulletListItem true)
(deftag BulletList true)
(deftag Paragraph true)

(deftag Document true)


(extend-type clojure.lang.IPersistentVector
  Tag
  (tag [this] (first this)))


#_(ns-unmap *ns* 'walk)
(defmulti walk tag)


(defmethod walk :md/leaf
  [^Node node]
  [(tag node) (str (.getChars node))])


(defmethod walk :md/container
  [^Node node]
  (into [(tag node)]
        (map walk)
        (.getChildren node)))


(defn parse
  [^String s]
  (let [options (MutableDataSet.)
        ^Parser parser (.build (Parser/builder options))]
    (walk (.parse parser s))))


(defn xform-string
  [string f]
  (-> string
      (parse)
      (f)
      (mdr/render)))
