(ns mdcat.selector
  (:require
    [clojure.string :as str]
    [com.rpl.specter :as sp]
    [instaparse.core :as insta]
    [mdcat.markdown :as md]))


(def parser
  (insta/parser
    "selector = (symbol <opt_whitespace>)+
     symbol = #'(\\w|[.|*])+'
     opt_whitespace = #'\\h?'"))


(defn parse
  [s]
  (insta/parse parser s))


(defmulti ->apath first)


(defmethod ->apath :selector
  [[_ & selectors]]
  (mapv ->apath selectors))


(defn match-type
  [sym]
  (case (first sym)
    \. :shallow
    \* :deep
    :default))


(defn base
  [sym]
  (str/replace sym #"^[.*]" ""))


(defn deep-walker
  [pred]
  (sp/recursive-path [] path
                     (sp/if-path pred
                                 (sp/continue-then-stay [sp/ALL vector? path])
                                 [sp/ALL vector? path])))


(defmethod ->apath :symbol
  [[_ sym]]
  (let [pred (get {"list" md/bullet-list?
                   "item" md/bullet-list-item?
                   "paragraph" md/paragraph?}
                  (base sym))]
    [sp/ALL
     vector?
     (get {:shallow pred
           :default (sp/walker pred)
           :deep (deep-walker pred)}
          (match-type sym))]))


(defn selector
  [s]
  (->apath (parse s)))


(comment

(let [document [:md/document
                [:md/heading
                 [:md/bullet-list
                  [:md/bullet-list-item [:md/paragraph [:md/text "foo"]]]
                  [:md/bullet-list-item [:md/paragraph [:md/text "bar"]]]
                  [:md/heading [:md/bullet-list-item "qux"]]]]
                [:md/paragraph [:md/text "baz"]]]
      selector (parse "list .item")]
  (sp/select (->apath selector) document))
)
