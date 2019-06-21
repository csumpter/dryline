(ns dryline.core
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [clojure.set]
            [clojure.spec.alpha :as s]
            [clojure.zip :as z]))

(s/def :AWS/PropertyTypes (constantly true))
(s/def :AWS.ResourceType.Attribute/ItemType string?)
(s/def :AWS.ResourceType.Attribute/PrimitiveItemType string?)
(s/def :AWS.ResourceType.Attribute/PrimitiveType #{"String"
                                                   "Long"
                                                   "Integer"
                                                   "Double"
                                                   "Boolean"
                                                   "Timestamp"
                                                   "Json"})
(s/def :AWS.ResourceType.Attribute/Type string?)
(s/def :AWS.ResourceType/Attribute (s/keys :req-un []
                                           :opt-un [:AWS.ResourceType.Attribute/ItemType
                                                    :AWS.ResourceType.Attribute/PrimitiveItemType
                                                    :AWS.ResourceType.Attribute/PrimitiveType
                                                    :AWS.ResourceType.Attribute/Type]))
(s/def :AWS.ResourceType/AttributeName keyword?)
(s/def :AWS.ResourceType/Attributes (s/map-of :AWS.ResourceType/AttributeName :AWS.ResourceType/Attribute))
(s/def :AWS.ResourceType/Documentation string?)
(s/def :AWS.ResourceType/Properties (constantly true))
(s/def :AWS/ResourceType (s/keys :req-un [:AWS.ResourceType/Documentation
                                          :AWS.ResourceType/Properties]
                                 :opt-un [:AWS.ResourceType/Attributes]))
(s/def :AWS/ResourceTypeName keyword?)
(s/def :AWS/ResourceTypes (s/map-of :AWS/ResourceTypeName :AWS/ResourceType))
(s/def :AWS/ResourceSpecificationVersion string?)
(s/def :AWS/Resources (s/keys :req [:AWS/PropertyTypes
                                    :AWS/ResourceSpecificationVersion
                                    :AWS/ResourceTypes]))

(def aws-spec (-> "resources/aws/us-east-spec.json"
                  slurp
                  (json/parse-string (fn [k] (-> k
                                                 (string/replace #"(.*)::" "$1/")
                                                 (string/replace #"::" ".")
                                                 keyword)))
                  (clojure.set/rename-keys {:PropertyTypes :AWS/PropertyTypes
                                            :ResourceTypes :AWS/ResourceTypes
                                            :ResourceSpecificationVersion :AWS/ResourceSpecificationVersion})))
(s/valid? :AWS/Resources aws-spec)

(defn namify
  [rtn [pn _]]
  (keyword (str (namespace rtn) "." (name rtn)) (name pn)))

(defn spec-code
  [[rtn rt]]
  (let [{req true opt false} (group-by #(get-in % [1 :Required]) (:Properties rt))]
    `(s/def ~rtn (s/keys :req ~(map (partial namify rtn) req)
                         :opt ~(map (partial namify rtn) opt)))))

(sequence (map (comp eval spec-code))
          (get-in aws-spec [:AWS/ResourceTypes]))
