(ns roomkey.dryline.core
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]))

(s/def :roomkey.dryline.aws/PropertyTypes (constantly true))

(s/def :roomkey.dryline.aws.resourcetype.Attribute/ItemType string?)

(s/def :roomkey.dryline.aws.resourcetype.Attribute/PrimitiveItemType string?)

(s/def :roomkey.dryline.aws.resourcetype.Attribute/PrimitiveType
  #{"String" "Long" "Integer" "Double" "Boolean" "Timestamp" "Json"})

(s/def :roomkey.dryline.aws.resourcetype.Attribute/Type string?)

(s/def :roomkey.dryline.aws.ResourceType/Attribute
  (s/keys :req-un []
          :opt-un [:roomkey.dryline.aws.resourcetype.Attribute/ItemType
                   :roomkey.dryline.aws.resourcetype.Attribute/PrimitiveItemType
                   :roomkey.dryline.aws.resourcetype.Attribute/PrimitiveType
                   :roomkey.dryline.aws.resourcetype.Attribute/Type]))

(s/def :roomkey.dryline.aws.ResourceType/AttributeName keyword?)

(s/def :roomkey.dryline.aws.ResourceType/Attributes
  (s/map-of :roomkey.dryline.aws.ResourceType/AttributeName
            :roomkey.dryline.aws.ResourceType/Attribute))

(s/def :roomkey.dryline.aws.ResourceType/Documentation string?)

(s/def :roomkey.dryline.aws.ResourceType/Properties (constantly true))

(s/def :roomkey.dryline.aws/ResourceType
  (s/keys :req-un [:roomkey.dryline.aws.ResourceType/Documentation
                   :roomkey.dryline.aws.ResourceType/Properties]
          :opt-un [:roomkey.dryline.aws.ResourceType/Attributes]))

(s/def :roomkey.dryline.aws/ResourceTypeName keyword?)

(s/def :roomkey.dryline.aws/ResourceTypes
  (s/map-of :roomkey.dryline.aws/ResourceTypeName
            :roomkey.dryline.aws/ResourceType))

(s/def :roomkey.dryline.aws/ResourceSpecificationVersion string?)

(s/def :roomkey.dryline.aws/Resources
  (s/keys :req-un [:roomkey.dryline.aws/PropertyTypes
                   :roomkey.dryline.aws/ResourceSpecificationVersion
                   :roomkey.dryline.aws/ResourceTypes]))

(def ^:private prefix "roomkey.dryline")

(defn dryline-keyword
  [resource-type-name]
  (let [[top-level-service service type] (string/split resource-type-name #"::")
        [type subtype] (string/split type #"\.")
        service-prefix (string/join \. [prefix
                                        (string/lower-case top-level-service)
                                        (string/lower-case service)])]
    (if subtype
      (keyword (str service-prefix \. type) subtype)
      (keyword service-prefix type))))

(defn parse-spec
  [rdr]
  (json/parse-stream rdr
                     (fn [k] (if (re-matches #".+::.+::.+" k)
                               (dryline-keyword k)
                               (keyword k)))))

(defn namify
  [rtn [pn _]]
  (keyword (str (namespace rtn) "." (name rtn)) (name pn)))

(defn spec-code
  [[rtn rt]]
  (let [{req true opt false} (group-by #(get-in % [1 :Required]) (:Properties rt))]
    `(s/def ~rtn (s/keys :req-un ~(map (partial namify rtn) req)
                         :opt-un ~(map (partial namify rtn) opt)))))

(defn gen-resource-type-specs
  [rdr]
  (sequence (map (comp eval spec-code))
            (:ResourceTypes (parse-spec rdr))))

(comment
  ;; These are for ease of development and should be removed before release
  (def ^:private local-spec-file "resources/aws/us-east-spec.json")

  (defn parse-spec-local
    []
    (parse-spec (io/reader local-spec-file)))

  (defn gen-resource-type-specs-local
    []
    (sequence (map (comp eval spec-code))
              (:ResourceTypes (parse-spec-local)))))
