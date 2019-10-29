(ns roomkey.dryline.specs
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [roomkey.dryline.parse :as parse]))

(def ^:private prefix "roomkey")

(defn dryline-keyword
  "Converts strings of form AWS::<Service>::<Resource> to namespaced keywords"
  [type-name]
  (let [[top-level-service service type] (string/split type-name #"::")
        [type subtype] (string/split type #"\.")
        service-prefix (string/join \. [prefix
                                        (string/lower-case top-level-service)
                                        (string/lower-case service)])]
    (if subtype
      (keyword (str service-prefix \. type) subtype)
      (keyword service-prefix type))))

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
  [type-name t]
  (case t
    "Tag" :roomkey.aws/Tag
    (append-to-keyword (dryline-keyword type-name) t)))

(defn- property-collection-predicate
  "Returns the predicate or reference for the collection type"
  [type-name {:keys [ItemType PrimitiveItemType]}]
  (if PrimitiveItemType
    (primitive-type->predicate PrimitiveItemType)
    (type-reference type-name ItemType)))

(defn- property-predicate
  "Returns the predicate for a given property"
  [type-name {:keys [DuplicatesAllowed
                     ItemType
                     PrimitiveItemType
                     PrimitiveType
                     Type]
        :as property}]
  (if PrimitiveType (primitive-type->predicate PrimitiveType)
      (case Type
        "List" (eval `(s/coll-of ~(property-collection-predicate type-name property)
                                 :distinct ~(not DuplicatesAllowed)))
        "Map" (eval `(s/map-of string? ~(property-collection-predicate type-name property)))
        (constantly true)
        ;; TODO need to generate property type specs before they can be referenced here
        #_(type-reference type-name Type))))

(defn gen-property-spec
  "Generates a spec for a property found in a resource type"
  [type-name [pn property]]
  (let [is-tag? (= type-name "Tag")
        spec-name (append-to-keyword (if is-tag? (keyword type-name) (dryline-keyword type-name)) pn)]
    (eval `(s/def ~spec-name ~(property-predicate type-name property)))))

(defn- namify
  [type-name [pn _]]
  (append-to-keyword (dryline-keyword type-name) pn))

(defn gen-resource-type-spec
  "Generates a spec for a resource type as well as all of the properties defined
  in its specification."
  [[type-name {:keys [Properties]}]]
  (let [property-specs (map (partial gen-property-spec type-name) Properties)
        {req true opt false} (group-by #(get-in % [1 :Required]) Properties)
        resource-spec (eval `(s/def ~(dryline-keyword type-name)
                               (s/keys :req-un ~(mapv (partial namify type-name) req)
                                       :opt-un ~(mapv (partial namify type-name) opt))))]
    (conj property-specs
          resource-spec)))

(defn gen-resource-type-specs
  "Generates all of the specs for resources types given a AWS CloudFormation
  specification as a reader"
  [parsed-spec]
  (mapcat gen-resource-type-spec (:ResourceTypes parsed-spec)))

(comment
  ;; These are for ease of development and should be removed before release
  (def ^:private local-spec-file "resources/aws/us-east-spec.json")

  (defn parse-spec-local
    []
    (parse/parse (io/reader local-spec-file)))

  (defn gen-resource-type-specs-local
    []
    (gen-resource-type-specs (io/reader local-spec-file))))

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
