(ns roomkey.dryline.specs
  "Defines Clojure specs from AWS resource and property type specifications"
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [roomkey.dryline.walk :as walk]
            [roomkey.dryline.keywords :as kws]))

(s/def ::json
  (s/or :string string?
        :integer integer?
        :double double?
        :boolean boolean?
        :vector (s/coll-of ::json :kind vector?)
        :map (s/map-of string? ::json)))

(def primitive-type->spec
  "A map from CloudFormation PrimitiveType to Clojure predicates"
  {"String" 'string?
   "Long" 'int?
   "Integer" 'int?
   "Double" 'double?
   "Boolean" 'boolean?
   "Timestamp" 'inst?
   "Json" ::json})

(def ^:deprecated primitive-type->predicate primitive-type->spec)

(defn ^:deprecated dryline-keyword
  "Converts strings of form AWS::<Service>::<Resource> to namespaced keywords"
  [type-name]
  (case type-name
    "Tag" :roomkey.aws/Tag
    (let [[top-level-service service type] (string/split type-name #"::")
          [type subtype] (string/split type #"\.")
          service-prefix (string/join \. ["roomkey"
                                          (string/lower-case top-level-service)
                                          (string/lower-case service)])]
      (if subtype
        (keyword (str service-prefix \. type \. subtype) subtype)
        (keyword service-prefix type)))))

(defn- item-type-spec
  "Returns the spec for the type of an item in a collection"
  [primitive-type-mapping type-identifier {:keys [ItemType PrimitiveItemType]}]
  (if PrimitiveItemType
    (primitive-type-mapping PrimitiveItemType)
    (kws/referenced-property-type-keyword type-identifier ItemType)))

(defn- property-spec
  "Returns the spec for a property"
  [primitive-type-mapping type-identifier {:keys [DuplicatesAllowed
                                                  PrimitiveType
                                                  Type]
                                           :as property}]
  (if PrimitiveType
    (primitive-type-mapping PrimitiveType)
    (case Type
      "List" `(clojure.spec.alpha/coll-of
               ~(item-type-spec
                 primitive-type-mapping
                 type-identifier
                 property)
               :distinct ~(not DuplicatesAllowed))
      "Map" `(clojure.spec.alpha/map-of
              string?
              ~(item-type-spec
                primitive-type-mapping
                type-identifier
                property))
      (kws/referenced-property-type-keyword type-identifier Type))))

(defn- add-property-spec
  "Adds a spec for a property specification to the registry"
  [spec-keyword primitive-type-mapping type-identifier property-specification]
  (eval `(clojure.spec.alpha/def ~spec-keyword
           ~(property-spec primitive-type-mapping
                           type-identifier
                           property-specification))))

(defn- required-and-optional-property-specs
  "Returns a tuple of [required-property-specs, optional-property-specs]"
  [type-property-keyword-fn type-identifier properties]
  (reduce-kv (fn [acc property-identifier {:keys [Required]}]
               (update acc
                       (if Required 0 1)
                       conj
                       (type-property-keyword-fn type-identifier
                                                 property-identifier)))
             [[] []]
             properties))

(defn- add-type-spec
  "Adds a spec for a resource type or property type to the registry. Specs for
  each property described in the type specification are also added. The names
  of specs are defined by the function passed in to the first two args. Do not
  call this function directly. Instead call `add-resource-type-spec` or
  `add-property-type-spec`"
  [type-keyword-fn
   type-property-keyword-fn
   primitive-type-mapping
   [type-identifier {:keys [Properties] :as _type-specification}]]
  (let [[required optional] (required-and-optional-property-specs
                             type-property-keyword-fn
                             type-identifier
                             Properties)
        spec-name (type-keyword-fn type-identifier)]
    (doseq [[property-identifier property-specification] Properties]
      (add-property-spec (type-property-keyword-fn type-identifier
                                                   property-identifier)
                         primitive-type-mapping
                         type-identifier
                         property-specification))
    (eval `(clojure.spec.alpha/def ~spec-name
             (clojure.spec.alpha/keys :req-un ~(when (seq required) required)
                                      :opt-un ~(when (seq optional) optional))))))

(defn- add-property-type-spec
  "Adds a spec for a property type to the registry"
  [primitive-type-mapping property-type]
  (add-type-spec kws/property-type-keyword
                 kws/property-type-property-keyword
                 primitive-type-mapping
                 property-type))

(defn- add-resource-type-spec
  "Adds a spec for a resource type to the registry"
  [primitive-type-mapping resource-type]
  (add-type-spec kws/resource-type-keyword
                 kws/resource-type-property-keyword
                 primitive-type-mapping
                 resource-type))

(defn- add-property-type-specs
  "Adds all of the specs for property types to the registry"
  [property-types primitive-type-mapping]
  (doseq [root-property-type (walk/root-property-types property-types)]
    (walk/property-type-walk (partial add-property-type-spec primitive-type-mapping)
                        (walk/property-type-zipper property-types root-property-type))))

(defn- add-resource-type-specs
  "Adds all of the specs for resource types to the registry"
  [resource-types primitive-type-mapping]
  (doseq [resource-type resource-types]
    (add-resource-type-spec primitive-type-mapping resource-type)))

(defn add-specs
  "Adds specs for all resource types and property types using the supplied
  primitive type mapping to the registry"
  ([parsed-spec primitive-type-mapping]
   (add-property-type-specs (:PropertyTypes parsed-spec) primitive-type-mapping)
   (add-resource-type-specs (:ResourceTypes parsed-spec) primitive-type-mapping)))

(def ^:deprecated gen-specs add-specs)
