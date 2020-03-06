(ns roomkey.dryline.util
  (:require [clojure.string]))

(def ^:private prefix "roomkey")

(defn dryline-keyword
  "Converts strings of form AWS::<Service>::<Resource> to namespaced keywords"
  [type-name]
  (case type-name
    "Tag" :roomkey.aws/Tag
    (let [[top-level-service service type] (clojure.string/split type-name #"::")
          [type subtype] (clojure.string/split type #"\.")
          service-prefix (clojure.string/join \. [prefix
                                          (clojure.string/lower-case top-level-service)
                                          (clojure.string/lower-case service)])]
      (if subtype
        (keyword (str service-prefix \. type \. subtype) subtype)
        (keyword service-prefix type)))))

