(ns roomkey.dryline.specs
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [roomkey.dryline.parse :as parse]
            [clojure.zip :as z]))

(def ^:private prefix "roomkey")

(defn dryline-keyword
  "Converts strings of form AWS::<Service>::<Resource> to namespaced keywords"
  [type-name]
  (case type-name
    "Tag" :roomkey.aws/Tag
    (let [[top-level-service service type] (string/split type-name #"::")
          [type subtype] (string/split type #"\.")
          service-prefix (string/join \. [prefix
                                          (string/lower-case top-level-service)
                                          (string/lower-case service)])]
      (if subtype
        (keyword (str service-prefix \. type) subtype)
        (keyword service-prefix type)))))

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

(defn- namify
  [type-name [pn _]]
  (append-to-keyword (dryline-keyword type-name) pn))

(defn gen-property-spec
  "Generates a spec for a property found in a resource type"
  [type-name [pn property]]
  (let [spec-name (append-to-keyword (dryline-keyword type-name) pn)]
    (eval `(s/def ~spec-name ~(property-predicate type-name property)))))

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

(defn primitive?
  [{:keys [PrimitiveType PrimitiveItemType]}]
  (boolean (or PrimitiveType PrimitiveItemType)))

(defn gen-spec-if-not-present
  "Returns the keyword of the generated spec only generating the spec if
  it is not already in the registry"
  [[property-name property]]
  (let [spec-name (append-to-keyword (dryline-keyword property-name) property)]
    (eval `(s/def ~spec-name ~(property-predicate property-name property)))
    spec-name))

;;(gen-spec-if-not-present ["AWS::AutoScalingPlans::ScalingPlan.PredefinedScalingMetricSpecification" :ResourceLabel])

;;(gen-spec-if-not-present ["Tag" :Value])

;;(zip/branch? zipper)

#_(defn gen-property-specs* [parsed-spec]
  (let [right-exists? #(not= (z/right loc) nil)
        came-from-up? (atom false)
        spec-zipper (zip/zipper
                    (fn [n] (vector? n))
                    (fn [n] (second n))
                    (fn [n children] n)
                    parsed-spec)]
      (loop [loc (zip/branch? spec-zipper)]
        (if (primitive? loc)
          (do (gen-spec-if-not-present loc)
              (if right-exists? loc)))
        (recur ))))

(defn root-properties
  [{:keys [PropertyTypes]}]
  (let [ptns (map (comp dryline-keyword first) PropertyTypes)
        referenced-ptns
        (mapcat (fn [[property-type-name property-type]]
                  (prn property-type-name)
                  (map (fn [[property-name property]]
                         (append-to-keyword (dryline-keyword property-type-name)
                                            property-name))
                       (:Properties property-type)))
                PropertyTypes)]
    ptns
    referenced-ptns))

;; (root-properties (parse-spec-local))

(comment
  ;; These are for ease of development and should be removed before release
  (def ^:private local-spec-file "resources/aws/S3BucketSpecification.json")

  (defn parse-spec-local []
    (parse/parse (io/reader local-spec-file)))

  (defn gen-resource-type-specs-local []
    (gen-resource-type-specs (parse/parse (io/reader local-spec-file))))

  (defn gen-property-type-specs-local []
    (parse/parse (io/reader local-spec-file))))

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
