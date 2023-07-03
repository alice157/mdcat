(ns mdcat.markdown
  (:require
    [clojure.string :as str])
  (:import
    (com.vladsch.flexmark.ast
      BulletList
      BulletListItem
      Emphasis
      Heading
      ListBlock
      ListItem
      Paragraph
      SoftLineBreak
      Text)
    com.vladsch.flexmark.parser.Parser
    (com.vladsch.flexmark.util.ast
      Document
      Node
      NodeVisitor
      VisitHandler
      Visitor)
    (com.vladsch.flexmark.util.data
      DataHolder
      MutableDataSet)))


(set! *warn-on-reflection* true)


(def ^:const INDENT "    ")
(def ^:const BULLET "- ")


(defn indent-str
  [level]
  (apply str (repeat level INDENT)))


(defprotocol Tag
  (tag [this]))


(extend-protocol Tag
  Object
  (tag [this] [:md/unknown (type this)])

  BulletList
  (tag [this] :md/bullet-list)

  Document
  (tag [this] :md/document)
  
  BulletListItem
  (tag [this] :md/bullet-list-item)
  
  Paragraph
  (tag [this] :md/paragraph)
  
  Text
  (tag [this] :md/text)

  Emphasis
  (tag [this] :md/emphasis)
  
  SoftLineBreak
  (tag [this] :md/soft-line-break)
  
  Heading
  (tag [this] :md/heading))


(derive :md/text :md/leaf)
(derive :md/emphasis :md/leaf)
(derive :md/soft-line-break :md/leaf)

(derive :md/heading :md/newline-container)
(derive :md/bullet-list-item :md/newline-container)
(derive :md/bullet-list :md/newline-container)

(derive :md/paragraph :md/inline-container)

(derive :md/document :md/section-container)

(derive :md/newline-container :md/container)
(derive :md/inline-container :md/container)
(derive :md/section-container :md/container)


(ns-unmap *ns* 'walk)
(defmulti walk tag)


(defmethod walk :md/leaf
  [^Node node]
  [(tag node) (str (.getChars node))])


(defmethod walk :md/container
  [^Node node]
  (into [(tag node)]
        (map walk)
        (.getChildren node)))


(defmethod walk :default
  [node]
  (tag node))


(defn parse
  [^String s]
  (let [options (MutableDataSet.)
        ^Parser parser (.build (Parser/builder options))]
    (.parse parser s)))


(ns-unmap *ns* 'render)
(defmulti render (fn [node args]
                   (first node)))


(defmethod render nil
  [_ _]
  "")


(defmethod render :md/section-container
  [[tag & children] args]
  (str
    (str/join "\n\n"
              (map #(render % args) children))
    "\n"))


(defmethod render :md/newline-container
  [[tag & children] args]
  (str/join "\n"
            (map #(render % args) children)))


(defmethod render :md/inline-container
  [[tag & children] args]
  (str/join "" (map #(render % args) children)))


(defmethod render :md/heading
  [[tag & children] args]
  (render (into [:md/newline-container
                 [:md/paragraph [:md/text (str (apply str (repeat (:heading-level args 1) "#"))
                                               " ")]
                                 (first children)]]
                (rest children))
          (update args :heading-level (fnil inc 1))))


(defmethod render :md/bullet-list-item
  [[tag & children] args]
  (render (into [:md/newline-container
                 [:md/paragraph [:md/text BULLET] (first children)]]
                (rest children))
          args))


(defn indent-list-child
  [item-text level]
  (->>
    (str/replace item-text #"\n" (str "\n" (indent-str level)))
    (str (indent-str level))))


(defmethod render :md/bullet-list
  [[tag & children] args]
  (let [bullet-level (:bullet-level args 0)]
    (->
      (into [:md/newline-container]
            (comp (map #(render % (assoc args :bullet-level (inc bullet-level))))
                  (map #(indent-list-child % bullet-level))
                  (map #(vector :md/leaf %)))
            children)
      (render args))))


(defmethod render :md/leaf
  [[tag & children] args]
  (str/join "" children))


(defn xform-string
  [string f]
  (-> string
      (parse)
      (walk)
      (f)
      (render {})))
