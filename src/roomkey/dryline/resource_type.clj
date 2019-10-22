(ns roomkey.dryline.resource-type
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]))

;; The following specs are for validating the specification file from AWS
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
  "Converts strings of form AWS::<Service>::<Resource> to namespaced keywords"
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
  "Parses an AWS CloudFormation specification passed in as a reader."
  [rdr]
  (json/parse-stream rdr
                     (fn [k]
                       (cond
                         ;; Specs with only one resource defined
                         ;; have a singular key so we change it
                         ;; to plural here. Thx AWS.
                         (= k "ResourceType") :ResourceTypes
                         (re-matches #".+::.+::.+" k) (dryline-keyword k)
                         :else (keyword k)))))

(def ^:private primitive-type->predicate
  "A map from CF PrimitiveType to clojure predicates"
  {"String" string?
   "Long" int?
   "Integer" int?
   "Double" double?
   "Boolean" boolean?
   "Timestamp" inst?
   "Json" map?})

(defn- append-to-keyword
  "Returns a keyword with a namespace equal to the name of kw appended to its
  namespace with a period and a name of suffix"
  [kw suffix]
  (keyword (str (namespace kw) \. (name kw)) (name suffix)))

(defn- type-reference
  [rtn t]
  (case t
    "Tag" :roomkey.dryline.aws/Tag
    (append-to-keyword rtn t)))

(defn- property-collection-predicate
  "Returns the predicate or reference for the collection type"
  [rtn {:keys [ItemType PrimitiveItemType]}]
  (if PrimitiveItemType
    (primitive-type->predicate PrimitiveItemType)
    (type-reference rtn ItemType)))

(defn- property-predicate
  "Returns the predicate for a given property"
  [rtn {:keys [DuplicatesAllowed
               ItemType
               PrimitiveItemType
               PrimitiveType
               Type]
        :as property}]
  (if PrimitiveType (primitive-type->predicate PrimitiveType)
      (case Type
        "List" (eval `(s/coll-of ~(property-collection-predicate rtn property)
                                 :distinct ~(not DuplicatesAllowed)))
        "Map" (eval `(s/map-of string? ~(property-collection-predicate rtn property)))
        (constantly true)
        ;; TODO need to generate property type specs before they can be referenced here
        #_(type-reference rtn Type))))

(defn gen-property-spec
  "Generates a spec for a property found in a resource type"
  [rtn [pn property]]
  (let [spec-name (append-to-keyword rtn pn)]
    (eval `(s/def ~spec-name ~(property-predicate rtn property)))))

(defn- namify
  [rtn [pn _]]
  (append-to-keyword rtn pn))

(defn gen-resource-type-spec
  "Generates a spec for a resource type as well as all of the properties defined
  in its specification."
  [[rtn {:keys [Properties] :as rt}]]
  (let [property-specs (map (partial gen-property-spec rtn) Properties)
        {req true opt false} (group-by #(get-in % [1 :Required]) Properties)
        rtn-spec (eval `(s/def ~rtn (s/keys :req-un ~(mapv (partial namify rtn) req)
                                            :opt-un ~(mapv (partial namify rtn) opt))))]
    (conj property-specs
          rtn-spec)))

(defn gen-resource-type-specs
  "Generates all of the specs for resources types given a AWS CloudFormation
  specification as a reader"
  [rdr]
  (mapcat gen-resource-type-spec (:ResourceTypes (parse-spec rdr))))

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

(comment
  ;; Example Properties for testing
  (def queue-key :roomkey.dryline.aws.sqs/Queue)
  (def p1 {:PrimitiveType "String"})
  (def p2 {:Type "VPC"})
  (def p3 {:Type "List"
           :PrimitiveItemType "String"})
  (def p4 {:Type "List"
           :ItemType "VPC"})
  (def p5 {:Type "List"
           :ItemType "Tag"})
  (def p6 {:Type "Map"
           :PrimitiveItemType "Integer"})
  (def p7 {:Type "Map"
           :ItemType "VPC"})

  (def ps [p1 p2 p3 p4 p5 p6 p7]))
