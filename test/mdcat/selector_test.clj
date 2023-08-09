(ns mdcat.selector-test
  (:require
    [clojure.test :as t]
    [com.rpl.specter :as sp]
    [mdcat.selector :as sel]))


(def document
  [:md/document
   [:md/heading [:md/text "heading"]
    [:md/paragraph [:md/text "wobble"]]]
   [:md/bullet-list
    [:md/bullet-list-item [:md/paragraph [:md/text "foo"]]]
    [:md/bullet-list-item [:md/paragraph [:md/text "bar"]]]
    [:md/bullet-list-item [:md/bullet-list
                           [:md/bullet-list-item [:md/paragraph [:md/text "baz"]]]
                           [:md/bullet-list-item [:md/paragraph [:md/text "qux"]]]]]]])


(t/deftest sym
  (t/testing "symbol search"
    (t/testing "recursive symbol search"
      (t/is (= [[:md/paragraph [:md/text "wobble"]]
                [:md/paragraph [:md/text "foo"]]
                [:md/paragraph [:md/text "bar"]]
                [:md/paragraph [:md/text "baz"]]
                [:md/paragraph [:md/text "qux"]]]
               (sp/select (sel/selector "paragraph") document))
            "`paragraph` selects all paragraphs"))
    (t/testing "shallow symbol search"
      (t/is (= [[:md/bullet-list
                 [:md/bullet-list-item [:md/paragraph [:md/text "foo"]]]
                 [:md/bullet-list-item [:md/paragraph [:md/text "bar"]]]
                 [:md/bullet-list-item [:md/bullet-list
                                        [:md/bullet-list-item [:md/paragraph [:md/text "baz"]]]
                                        [:md/bullet-list-item [:md/paragraph [:md/text "qux"]]]]]]]
               (sp/select (sel/selector ".list") document))
            "`.list` selects only outer list"))
    (t/testing "compound symbol search"
      (t/is (= [[:md/paragraph [:md/text "foo"]]
                [:md/paragraph [:md/text "bar"]]]
               (sp/select (sel/selector "list .item .paragraph") document)))
      (t/is (= [[:md/paragraph [:md/text "foo"]]
                [:md/paragraph [:md/text "bar"]]
                [:md/paragraph [:md/text "baz"]]
                [:md/paragraph [:md/text "qux"]]]
               (sp/select (sel/selector "list paragraph") document))))
    (t/testing "regular search doesn't recurse past match"
      (t/is (= [[:md/paragraph
                 [:md/text "foo"]
                 [:md/paragraph [:md/text "bar"]]]]
               (sp/select (sel/selector "paragraph")
                          [:md/paragraph
                           [:md/text "foo"]
                           [:md/paragraph [:md/text "bar"]]]))))
    (t/testing "deep search recurses past match"
      (t/is (= [[:md/paragraph
                 [:md/text "foo"]
                 [:md/paragraph [:md/text "bar"]]]
                [:md/paragraph [:md/text "bar"]]]
               (sp/select (sel/selector "*paragraph")
                          [:md/paragraph
                           [:md/text "foo"]
                           [:md/paragraph [:md/text "bar"]]]))))))
