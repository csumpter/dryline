(ns roomkey.dryline.specs
  "Generates Clojure specs from AWS resource and property type specifications.

  Definitions:
    AWS Type Identifier: An identifier found in AWS specifications of the form
    <ServiceProvider>::<ServiceName>::<DataTypeName>. Dryline parses these
    identifiers as strings.

    ServiceProvider: The first value in a type identifier. Either AWS or Alexa.

    ServiceName: The name of the AWS service to which a resource or property
    belongs. E.g. S3, Lambda, EC2, ApiGateway.

    DataTypeName: The name of the resource or property type. DataTypeName
    references a property type if it of the form
    <ResourceTypeName>.<PropertyTypeName>.

    Resource Type: A top level CloudFormation type. Resource types are a tuple
    of [type-identifier, type-specification].

    Property Type: A sub-type of a resource type used to describe data structures
    that are not primitive. Resource types are a tuple of
    [type-identifier, type-specification]

    Type Specification: A map describing a resource type or property type.
    Contains the key Properties.

    Properties: A map of property-identifier -> property.

    Property Identifier: An identifier for a property of a resource type or
    property type. Dryline parses these identifiers as keywords.

    Property Specification: A map describing a property. Provides information
    about the type of the property and whether or not it is required.

    Primitive Type Mapping: A function that maps an AWS primitive type to a
    Clojure spec."
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.zip :as z]))

(s/def ::json
  (s/or :string string?
        :integer integer?
        :double double?
        :boolean boolean?
        :vector (s/coll-of ::json :kind vector?)
        :map (s/map-of string? ::json)))

;; TODO: rename to primitive-type->spec and update doc string
(def primitive-type->predicate
  "A map from CloudFormation PrimitiveType to Clojure predicates"
  {"String" 'string?
   "Long" 'int?
   "Integer" 'int?
   "Double" 'double?
   "Boolean" 'boolean?
   "Timestamp" 'inst?
   "Json" ::json})

(defn- split-type-identifier
  "Splits an AWS type identifier of the form into a tuple of
  (<serviceprovider> <servicename> <ResourceTypeName> ?<PropertyTypeName>).
  The tuple will contain a fourth element of PropertyTypeName iff DataTypeName
  contains a `.` signifying that it is a property type. Note that ServiceProvider
  and ServiceName will be returned in lower case in the tuple."
  [type-identifier]
  (-> (string/split type-identifier #"::")
      (update 0 string/lower-case)
      (update 1 string/lower-case)
      (update 2 string/split #"\.")
      flatten))

(def ^:private prefix "roomkey")

(defn ^:depricated dryline-keyword
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
        (keyword (str service-prefix \. type \. subtype) subtype)
        (keyword service-prefix type)))))

(defn- make-namespace
  "Builds a dot concatenated namespace string from `args` with the first value
  being \"roomkey\""
  [& args]
  (string/join \. (into [prefix] args)))

(defn resource-type-keyword
  "Converts an AWS resource type identifier into a keyword of form
  :roomkey.<serviceprovider>.<servicename>/<ResourceTypeName> "
  [resource-type-identifier]
  (let [[provider service resource-type-name]
        (split-type-identifier resource-type-identifier)]
    (keyword (make-namespace provider service)
             resource-type-name)))

(defn resource-type-property-keyword
  "Returns a keyword for a property of a resource type of form
  :roomkey.<serviceprovider.<servicename>.<ResourceTypeName>/<PropertyIdentifier>"
  [resource-type-identifier property-identifier]
  (let [[provider service resource-type]
        (split-type-identifier resource-type-identifier)]
    (keyword (make-namespace provider service resource-type)
             (name property-identifier))))

(defn property-type-keyword
  "Converts an AWS property type identifier to a keyword of form
  :roomkey.<serviceprovider>.<servicename>.<ResourceTypeName>.properties/<PropertyTypeName>"
  [property-type-identifier]
  (case property-type-identifier
    "Tag" :roomkey.aws/Tag
    (let [[provider service resource-type-name property-type-name]
          (split-type-identifier property-type-identifier)]
      (keyword (make-namespace provider service resource-type-name "properties")
               property-type-name))))

(defn property-type-property-keyword
  "Returns a keyword for a property of a property type of form
  :roomkey.<serviceprovider>.<servicename>.<ResourceTypeName>.properties.<PropertyTypeName>/<PropertyIdentifier>"
  [property-type-identifier property-identifier]
  (case property-type-identifier
    "Tag" (keyword "roomkey.aws.Tag" (name property-identifier))
    (let [[provider service resource-type-name property-type-name]
          (split-type-identifier property-type-identifier)]
      (keyword (make-namespace provider service resource-type-name "properties"
                               property-type-name)
               (name property-identifier)))))

(defn- referenced-property-type-spec
  "Returns the spec keyword for a referenced property type"
  [type-identifier property-identifier]
  (case property-identifier
    "Tag" :roomkey.aws/Tag
    (let [[provider service resource-type-name]
          (split-type-identifier type-identifier)]
      (keyword (make-namespace provider service resource-type-name "properties")
               (name property-identifier)))))

(defn- item-type-spec
  "Returns the spec for the type of an item in a collection"
  [primitive-type-mapping type-identifier {:keys [ItemType PrimitiveItemType]}]
  (if PrimitiveItemType
    (primitive-type-mapping PrimitiveItemType)
    (referenced-property-type-spec type-identifier ItemType)))

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
      (referenced-property-type-spec type-identifier Type))))

;; TODO is this where we should check before re-defining a spec?
(defn- add-property-spec
  "Adds a spec for a property to the registry"
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

(defn add-property-type-spec
  "Adds a spec for a property type to the registry"
  [primitive-type-mapping property-type]
  (add-type-spec property-type-keyword
                 property-type-property-keyword
                 primitive-type-mapping
                 property-type))

(defn add-resource-type-spec
  "Adds a spec for a resource type to the registry"
  [primitive-type-mapping resource-type]
  (add-type-spec resource-type-keyword
                 resource-type-property-keyword
                 primitive-type-mapping
                 resource-type))

(defn- primitive-property-specification?
  "Returns true if a property specification is primitive; false otherwise. A
  property is primitive if it defines either a PrimitiveType or a
  PrimitiveItemType"
  [{:keys [PrimitiveType PrimitiveItemType] :as _property-specification}]
  (boolean (or PrimitiveType PrimitiveItemType)))

(defn- referenced-property-type-identifier
  "Returns the property type identifier for a that is referenced by a property
  specification"
  [property-type-identifier {:keys [ItemType Type] :as _property-specification}]
  (let [referenced-type (case Type
                          ("List" "Map") ItemType
                          Type)]
    (case referenced-type
      "Tag" referenced-type
      (str (first (string/split property-type-identifier #"\."))
           \.
           referenced-type))))

(defn- referenced-property-type-identifiers
  "Returns a collection of the property type identifiers that are referenced by
  a property type"
  [[property-type-identifier property-type-specification :as _property-type]]
  (sequence (comp (remove primitive-property-specification?)
                  (map (partial referenced-property-type-identifier
                                property-type-identifier)))
            (vals (:Properties property-type-specification))))

(defn- root-property-types
  "Given a collection of all property types, returns the property types which
  are not referenced by any other property type"
  [property-types]
  (remove (comp (set (mapcat referenced-property-type-identifiers
                             property-types))
                first)
          property-types))

(defn- property-type-zipper
  "Returns a zipper that can walk a graph of dependent property-types"
  [property-types root-property-type]
  (z/zipper

   ;; A property type is a leaf node if it has any non-primitive properties
   (fn [[_ {:keys [Properties]}]]
     (boolean (seq (remove (comp primitive-property-specification? second)
                           Properties))))

   ;; The children of a property-type node are the property types referenced
   ;; in its properties
   (fn [property-type]
     (select-keys property-types
                  (referenced-property-type-identifiers property-type)))

   (fn [n _children] n)
   root-property-type))

(defn- property-type-walk
  "Walks the dependency graph of property types ensuring that specs are
  generated in the correct order."
  [spec-generator-fn zipper]
  (loop [loc zipper
         came-up? false]
    (cond
      (nil? loc) nil

      came-up?
      (do
        (spec-generator-fn (z/node loc))
        (if (z/right loc)
          (recur (z/right loc) false)
          (recur (z/up loc) true)))

      :else
      (if (and (z/branch? loc)
               (not= (z/node (z/down loc))
                     (z/node loc)))
        (recur (z/down loc) false)
        (do
          (spec-generator-fn (z/node loc))
          (if (z/right loc)
            (recur (z/right loc) false)
            (recur (z/up loc) true)))))))

(defn- gen-property-type-specs
  "Generates all of the specs for property types"
  [property-types primitive-type-mapping]
  (doseq [root-property-type (root-property-types property-types)]
    (property-type-walk (partial add-property-type-spec primitive-type-mapping)
                        (property-type-zipper property-types root-property-type))))

(defn- gen-resource-type-specs
  "Generates all of the specs for resource types"
  [resource-types primitive-type-mapping]
  (doseq [resource-type resource-types]
    (add-resource-type-spec primitive-type-mapping resource-type)))

(defn gen-specs
  "Generates specs for all resource types and property types using the supplied
  primitive type mapping"
  ([parsed-spec primitive-type-mapping]
   (gen-property-type-specs (:PropertyTypes parsed-spec) primitive-type-mapping)
   (gen-resource-type-specs (:ResourceTypes parsed-spec) primitive-type-mapping)))
