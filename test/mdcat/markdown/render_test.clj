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
             (mdr/render [:md/bullet-list-item "foo"
                          [:md/bullet-list-item "bark"]
                          [:md/bullet-list-item "meow"]
                          [:md/bullet-list-item "meep"]])))))
