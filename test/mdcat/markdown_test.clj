(ns mdcat.markdown-test
  (:require
    [clojure.test :as t]
    [mdcat.markdown :as md]))


(t/deftest parse-document
  (t/is (= [:md/document [:md/paragraph [:md/text "wibble wobble"]]]
           (md/parse
             (str "wibble wobble"))))
  (t/is (= [:md/document
            [:md/paragraph
             [:md/text "foo"]
             [:md/soft-line-break "\n"]
             [:md/text "bar"]]
            [:md/paragraph
             [:md/text "baz"]
             [:md/soft-line-break "\n"]
             [:md/text "qux"]]]
           (md/parse
             (str "foo\n"
                  "bar\n"
                  "\n"
                  "baz\n"
                  "qux\n"))))
  (t/is (= [:md/document
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
                 [:md/text "fable"]]]]]]]
           (md/parse
             (str "- foo bar baz\n"
                  "foo bar\n"
                  "  - fibble fobble\n"
                  "  fable")))))
