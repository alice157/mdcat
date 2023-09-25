(ns mdcat.markdown.render-test
  (:require
    [clojure.test :as t]
    [mdcat.markdown.render :as mdr]))


(t/deftest fragments
  (t/testing "text"
    (t/is (= "wibble wabble wobble"
             (mdr/render [:md/text "wibble wabble wobble"]))))
  (t/testing "nested bullet list items"
    (t/is (= "- foo\n  - bark\n  - meow\n  - meep"
             (mdr/render [:md/bullet-list-item [:md/text "foo"]
                          [:md/bullet-list-item [:md/text "bark"]]
                          [:md/bullet-list-item [:md/text "meow"]]
                          [:md/bullet-list-item [:md/text "meep"]]])))))


(t/deftest documents
  (t/is (= (str "foo\n"
                "bar\n"
                "\n"
                "baz\n"
                "qux\n")
           (mdr/render
             [:md/document
              [:md/paragraph
               [:md/text "foo"]
               [:md/soft-line-break "\n"]
               [:md/text "bar"]]
              [:md/paragraph
               [:md/text "baz"]
               [:md/soft-line-break "\n"]
               [:md/text "qux"]]])))
  (t/is (= (str "- foo bar baz\n"
                "foo bar\n"
                "  - fibble fobble\n"
                "fable\n")
           (mdr/render
             [:md/document
              [:md/bullet-list
               [:md/bullet-list-item
                [:md/paragraph
                 [:md/text "foo bar baz"]
                 [:md/soft-line-break "\n"]
                 [:md/text "foo bar"]]
                [:md/bullet-list
                 [:md/bullet-list-item
                  [:md/paragraph
                   [:md/text "fibble fobble"]
                   [:md/soft-line-break "\n"]
                   [:md/text "fable"]]]]]]]))))
