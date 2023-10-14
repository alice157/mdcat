(ns mdcat.selector-test
  (:require
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
                          [:md/document
                           [:md/paragraph
                            [:md/text "foo"]
                            [:md/paragraph [:md/text "bar"]]]]))))
    (t/testing "deep search recurses past match"
      (t/is (= [[:md/paragraph [:md/text "bar"]]
                [:md/paragraph
                 [:md/text "foo"]
                 [:md/paragraph [:md/text "bar"]]]]
               (sp/select (sel/selector "*paragraph")
                          [:md/document
                           [:md/paragraph
                            [:md/text "foo"]
                            [:md/paragraph [:md/text "bar"]]]]))))
    (t/testing "repeated symbols don't match outer"
      (t/is (= [[:md/bullet-list
                 [:md/bullet-list-item [:md/text "bar"]]]]
               (sp/select (sel/selector "list list")
                          [:md/document
                           [:md/bullet-list
                            [:md/bullet-list-item [:md/text "foo"]
                             [:md/bullet-list
                              [:md/bullet-list-item [:md/text "bar"]]]]
                            [:md/bullet-list-item [:md/text "baz"]]]]))))
    (t/testing "repeated symbols don't match outer, shallow"
      (t/is (= [[:md/paragraph [:md/text "foo"]]]
               (sp/select (sel/selector "paragraph .paragraph")
                          [:md/document
                           [:md/paragraph
                            [:md/paragraph [:md/text "foo"]]]]))))))

(t/deftest transform
  (t/testing "shallow transforms"
    (t/is (= [:md/document
              [:md/heading [:md/text "heading"]
               [:md/paragraph [:md/text "wobble"]]]
              [:md/bullet-list
               [:md/bullet-list-item [:md/paragraph [:md/text "foo"]]]
               [:md/bullet-list-item [:md/paragraph [:md/text "bar"]]]
               [:md/bullet-list-item [:md/bullet-list
                                      [:md/bullet-list-item [:md/paragraph [:md/text "baz"]]]
                                      [:md/bullet-list-item [:md/paragraph [:md/text "qux"]]]]]
               :added]]
             (sp/transform
               (sel/selector ".list")
               #(conj % :added)
               document))))
  (t/testing "default transforms"
    (t/is (= [:md/document
              [:md/paragraph
               [:md/text "foo"]
               [:md/paragraph [:md/text "bar"]]
               :added]]
             (sp/transform
               (sel/selector "paragraph")
               #(conj % :added)
               [:md/document
                [:md/paragraph
                 [:md/text "foo"]
                 [:md/paragraph [:md/text "bar"]]]]))))
  (t/testing "deep transforms"
    (t/is (= [:md/document
              [:nope
               [:yep
                [:md/text "bar"] [:md/text "baz"]]
               [:md/text "foo"]]]
             (sp/transform (sel/selector "*paragraph")
                           #(into [(if (md/text? (second %))
                                     :yep
                                     :nope)]
                                  (rest %))
                           [:md/document
                            [:md/paragraph
                             [:md/paragraph
                              [:md/text "bar"] [:md/text "baz"]]
                             [:md/text "foo"]]])))))
