(ns roomkey.dryline.resource-specification-version
  (:require [cheshire.core :as json]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]))

(s/def ::ResourceSpecificationVersion (s/and string? #(re-matches #"^([\d]+[.]?)+$" %)))

(comment
  ;; used for development - remove before release
  (defn read-json-file [path]
    (slurp path))
  (def aws-spec (json/parse-string (read-json-file "resources/aws/us-east-spec.json")
                                   (fn [k] (-> k
                                               (string/replace #"(.*)::" "$1/")
                                               (string/replace #"::" ".")
                                               keyword))))
  (s/valid? ::ResourceSpecificationVersion (:ResourceSpecificationVersion aws-spec)))
