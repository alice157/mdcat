(ns mdcat.selector-test
  (:require
    [clojure.data :refer [diff]]
    [clojure.test :as t]
    [com.rpl.specter :as sp]
    [mdcat.markdown :as md]
    [mdcat.selector :as sel]))


(def document
  [:md/document
   [:md/heading [:md/text "heading"]
    [:md/paragraph [:md/text "wobble"]]]
   [:md/bullet-list
    [:md/bullet-list-item [:md/paragraph [:md/text "foo"]]]
    [:md/bullet-list-item [:md/paragraph [:md/text "bar"]]]
    [:md/bullet-list-item [:md/bullet-list
                           [:md/bullet-list-item [:md/paragraph [:md/text "foo"]]]
                           [:md/bullet-list-item [:md/paragraph [:md/text "bar"]]]]]]])


(t/deftest sym
  (t/testing "recursive symbol search"
    (t/is (= [[:md/paragraph [:md/text "wobble"]]
              [:md/paragraph [:md/text "foo"]]
              [:md/paragraph [:md/text "bar"]]
              [:md/paragraph [:md/text "foo"]]
              [:md/paragraph [:md/text "bar"]]]
             (sp/select (sel/selector "paragraph") document))))
  (t/testing "shallow symbol search"
    (t/is (= [[:md/bullet-list
               [:md/bullet-list-item [:md/paragraph [:md/text "foo"]]]
               [:md/bullet-list-item [:md/paragraph [:md/text "bar"]]]
               [:md/bullet-list-item [:md/bullet-list
                                      [:md/bullet-list-item [:md/paragraph [:md/text "foo"]]]
                                      [:md/bullet-list-item [:md/paragraph [:md/text "bar"]]]]]]]
             (sp/select (sel/selector ".list") document)))))


(t/deftest compound
  (t/testing "compound symbol search"
    (is (= [:md/paragraph [:md/text "foo"]]))))
