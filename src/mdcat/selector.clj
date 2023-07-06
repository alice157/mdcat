(ns mdcat.selector
  (:require
    [com.rpl.specter :as sp]
    [instaparse.core :as insta]))


;; list>* should match all top level items of a list


(def parser
  (insta/parser
    "selector = symbol | binary_relationship | wildcard
     wildcard = '*'
     symbol = #'\\w+'
     opt_whitespace = #'\\h?'
     child_operator = '>'
     binary_operator = child_operator
     binary_relationship = selector <opt_whitespace> binary_operator <opt_whitespace> selector"))


(defn parse
  [s]
  (insta/parse parser s))


(defn select
  [selector md]
  (s/select [:md/document] md))


(comment
(let [document [:md/document [:md/bullet-list [:md/bullet-list-item "foo"] [:md/bullet-list-item "bar"]]]
      selector (parse "list")]
  (select selector document)))
