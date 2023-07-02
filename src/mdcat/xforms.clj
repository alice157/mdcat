(ns mdcat.xforms
  (:require
    [clojure.string :as str]
    [com.rpl.specter :as s]
    [java-time :as t]))


(def data
  [:md/document
   [:md/bullet-list
    [:md/bullet-list-item [:md/paragraph [:md/text "6/1"]]
     [:md/bullet-list
      [:md/bullet-list-item [:md/paragraph [:md/text "a"]]]
      [:md/bullet-list-item [:md/paragraph [:md/text "b"]]]
      [:md/bullet-list-item [:md/paragraph [:md/text "c"]]]]]
    [:md/bullet-list-item [:md/paragraph [:md/text "6/2"]]
     [:md/bullet-list
      [:md/bullet-list-item [:md/paragraph [:md/text "d"]]]
      [:md/bullet-list-item [:md/paragraph [:md/text "e"]]]
      [:md/bullet-list-item [:md/paragraph [:md/text "f"]]]]]]
   [:md/paragraph [:md/text "foo"]]])


(defn bullet-list?
  [md]
  (and (vector? md)
       (= :md/bullet-list (first md))))


(defn heading?
  [md]
  (and (vector? md)
       (= :md/heading? (first md))))


(defn bullet-list-item?
  [md]
  (and (vector? md)
       (= :md/bullet-list-item (first md))))


(def paragraph-contents [s/LAST s/LAST])
(def heading-contents [s/LAST])
(def top-level-headings [s/ALL heading?])
(def top-level-lists [s/ALL bullet-list?])


(defn bli-content
  [md]
  (and (bullet-list-item? md)
       (second md)))


(defn fixup-dates-current-year
  "Rewrites dates like 6/1 to 06/01/<current-year>"
  [s]
  (let [now (t/local-date)
       [month day] (map #(Integer/parseInt %) (str/split (str/trim s) #"/"))
        year (str (t/year now))]
    (format "%02d/%02d/%s" month day year)))


(defn unpack-list*
  [md]
  (prn md)
  (into [:md/heading (or (bli-content (second md))
                         (second md))]
        (drop 2 md)))


(defn reverse-container*
  [md]
  (into (empty md) (cons (first md) (reverse (rest md)))))


(comment
(->> data
     (s/transform top-level-lists reverse-container*)
     (s/transform [s/ALL bullet-list? s/ALL bullet-list-item?] unpack-list*)
     #_(s/select  top-level-headings)))
