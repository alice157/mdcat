(ns mdcat.task2
  (:require
    [com.rpl.specter :as sp]
    [mdcat.markdown :as md]
    [mdcat.selector :as sel]
    [puget.printer :as puget]))


(defn select
  [filepath selector]
  (let [matches (->> (slurp filepath)
                     (md/parse)
                     (sp/select (sel/selector selector))
                     (mapv md/render))]
    (doseq [match (interpose "\n\n-----\n\n" matches)]
      (println match))))

