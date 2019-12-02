(ns roomkey.dryline.specs
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.zip :as z]
            [roomkey.dryline.parse :as parse]))

(def ^:private prefix "roomkey")

(def spec-metadata (atom {}))

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
  {"String" 'string?
   "Long" 'int?
   "Integer" 'int?
   "Double" 'double?
   "Boolean" 'boolean?
   "Timestamp" 'inst?
   "Json" 'map?})

(defn- append-to-keyword
  "Returns a keyword with a namespace equal to the name of kw appended to its
  namespace with a period and a name of suffix"
  [kw suffix]
  (keyword (str (namespace kw) \. (name kw)) (name suffix)))

(defn- spec-reference
  "Returns a reference to a Clojure spec as a keyword"
  [type-name t]
  (case t
    "Tag" :roomkey.aws/Tag
    (-> type-name
        (string/split #"\.")
        first
        dryline-keyword
        (append-to-keyword t))))

(defn- property-collection-predicate
  "Returns the predicate or reference for the collection type"
  [type-name {:keys [ItemType PrimitiveItemType]}]
  (if PrimitiveItemType
    (primitive-type->predicate PrimitiveItemType)
    (spec-reference type-name ItemType)))

(defn- property-collection-predicate-2
  "Returns the predicate or reference for the collection type"
  [primitive-type-mapping type-name {:keys [ItemType PrimitiveItemType]}]
  (if PrimitiveItemType
    (primitive-type-mapping PrimitiveItemType)
    (spec-reference type-name ItemType)))

(defn- property-predicate
  "Returns the predicate for a given property"
  [type-name {:keys [DuplicatesAllowed
                     PrimitiveType
                     Type]
              :as property}]
  (if PrimitiveType
    (primitive-type->predicate PrimitiveType)
    (case Type
      "List" `(clojure.spec.alpha/coll-of
               ~(property-collection-predicate type-name property)
               :distinct ~(not DuplicatesAllowed))
      "Map" `(clojure.spec.alpha/map-of
              string?
              ~(property-collection-predicate type-name property))
      (spec-reference type-name Type))))

(defn- property-predicate-2
  "Returns the predicate for a given property"
  [primitive-type-mapping type-name {:keys [DuplicatesAllowed
                                            PrimitiveType
                                            Type]
                                     :as property}]
  (if PrimitiveType
    (primitive-type-mapping PrimitiveType)
    (case Type
      "List" `(clojure.spec.alpha/coll-of
               ~(property-collection-predicate-2 primitive-type-mapping
                                                 type-name
                                                 property)
               :distinct ~(not DuplicatesAllowed))
      "Map" `(clojure.spec.alpha/map-of
              string?
              ~(property-collection-predicate-2 primitive-type-mapping
                                                type-name
                                                property))
      (spec-reference type-name Type))))

(defn document-property!
  [spec-name property]
  (swap! spec-metadata assoc spec-name property))

(defn gen-property-spec
  "Generates a spec for a property found in a resource type"
  [type-name [pn property]]
  (let [spec-name (append-to-keyword (dryline-keyword type-name) pn)]
    (document-property! spec-name property)
    (eval `(clojure.spec.alpha/def ~spec-name
             ~(property-predicate type-name property)))))

(defn gen-property-spec-2
  "Generates a spec for a property found in a resource type"
  [primitive-type-mapping type-name [pn property]]
  (let [spec-name (append-to-keyword (dryline-keyword type-name) pn)]
    (document-property! spec-name property)
    (eval `(clojure.spec.alpha/def ~spec-name
             ~(property-predicate-2 primitive-type-mapping
                                    type-name
                                    property)))))

(defn- property-references
  [property-spec-reference properties]
  (reduce-kv (fn [acc property-name {:keys [Required]}]
               (update acc Required conj (property-spec-reference property-name)))
             {}
             properties))

(defn- document-type!
  [spec-name type-specification property-references]
  (swap! spec-metadata assoc spec-name (assoc type-specification :Properties property-references)))

(defn gen-type-spec
  "Generates a spec for a resource or property type as well as all of the
  properties defined in its specification."
  [[type-name {:keys [Properties] :as type-specification}]]
  (let [property-spec-reference (fn [property-name]
                                  (append-to-keyword (dryline-keyword type-name) property-name))
        property-specs (map (partial gen-property-spec type-name) Properties)
        {req true opt false} (property-references property-spec-reference Properties)
        spec-name (dryline-keyword type-name)
        resource-spec (eval `(clojure.spec.alpha/def ~spec-name
                               (clojure.spec.alpha/keys :req-un ~req
                                                        :opt-un ~opt)))]
    (document-type! spec-name type-specification (concat req opt))
    (conj property-specs
          resource-spec)))

(defn gen-type-spec-2
  "Generates a spec for a resource or property type as well as all of the
  properties defined in its specification."
  [primitive-type-mapping [type-name {:keys [Properties] :as type-specification}]]
  (let [property-spec-reference (fn [property-name]
                                  (append-to-keyword (dryline-keyword type-name) property-name))
        property-specs (map (partial gen-property-spec-2 primitive-type-mapping type-name)
                            Properties)
        {req true opt false} (property-references property-spec-reference Properties)
        spec-name (dryline-keyword type-name)
        resource-spec (eval `(clojure.spec.alpha/def ~spec-name
                               (clojure.spec.alpha/keys :req-un ~req
                                                        :opt-un ~opt)))]
    (document-type! spec-name type-specification (concat req opt))
    (conj property-specs
          resource-spec)))

(defn- primitive?
  "Returns true if a property is primitive. A primitive property can be defined
  only using the AWS CloudFormation primitive types and references no other types"
  [{:keys [PrimitiveType PrimitiveItemType]}]
  (boolean (or PrimitiveType PrimitiveItemType)))

(defn- non-primitive-type
  "Returns the referenced type from a property and returns nil
  if the property is primitive."
  [{:keys [Type ItemType]}]
  (case Type
    ("List" "Map") ItemType
    Type))

(defn- referenced-property-type
  "Returns the property type name referenced by a property"
  [property-type-name property]
  (let [npt (non-primitive-type property)]
    (case npt
      "Tag" npt
      (str (first (string/split property-type-name #"\."))
           \.
           npt))))

(defn- referenced-properties
  "Returns the properties which are references to other property types"
  [[property-type-name property-type]]
  (sequence (comp
             (remove (comp primitive? second))
             (map (fn [[property-name property]]
                    (referenced-property-type property-type-name property))))
            (:Properties property-type)))

(defn root-property-types
  "Returns a collection of root property type names. Root property
  types are those property types which are not referenced by other property
  types. The set will include property types whose properties are all primitive."
  [{:keys [PropertyTypes]}]
  (let [ptns (keys PropertyTypes)
        referenced-ptns (into #{} (mapcat referenced-properties) PropertyTypes)]
    (remove referenced-ptns ptns)))

(defn property-type-zipper
  "Returns a zipper that can walk a graph of dependent property-types"
  [root-property-type {:keys [PropertyTypes] :as parsed-spec}]
  (z/zipper
   (fn [[property-type-name property-type]]
     (boolean (seq (remove primitive? (vals (:Properties property-type))))))
   (fn [[property-type-name property-type]]
     (select-keys PropertyTypes
                  (sequence (comp (remove primitive?)
                                  (map (partial referenced-property-type
                                                property-type-name)))
                            (vals (:Properties property-type)))))
   (fn [n children] n)
   root-property-type))

(defn property-type-walk
  "Walks the dependency graph of property types ensuring that specs are
  generated in the correct order."
  [spec-generator-fn zipper]
  (loop [loc zipper
         came-up? false
         specs []]
    (cond
      (nil? loc) specs

      came-up?
      (let [new-specs (spec-generator-fn (z/node loc))]
        (if (z/right loc)
          (recur (z/right loc) false (concat specs new-specs))
          (recur (z/up loc) true (concat specs new-specs))))

      :else
      (if (and (z/branch? loc)
               (not= (z/node (z/down loc))
                     (z/node loc)))
        (recur (z/down loc) false specs)
        (let [new-specs (spec-generator-fn (z/node loc))]
          (if (z/right loc)
            (recur (z/right loc) false (concat specs new-specs))
            (recur (z/up loc) true (concat specs new-specs))))))))

(defn gen-property-type-specs
  "Generates all of the specs for PropertyTypes"
  [parsed-spec]
  (sequence (comp (map #(property-type-zipper % parsed-spec))
                  (mapcat (partial property-type-walk gen-type-spec)))
            (select-keys (:PropertyTypes parsed-spec)
                         (root-property-types parsed-spec))))

(defn gen-resource-type-specs
  "Generates all of the specs for ResourceTypes"
  [parsed-spec]
  (mapcat gen-type-spec (:ResourceTypes parsed-spec)))

(defn gen-specs
  "Generates specs for all PropertyTypes and ResourceTypes"
  [parsed-spec]
  (doall (concat (gen-property-type-specs parsed-spec)
                 (gen-resource-type-specs parsed-spec))))

(defn gen-property-type-specs-2
  "Generates all of the specs for PropertyTypes"
  [parsed-spec primitive-type-mapping]
  (sequence (comp (map #(property-type-zipper % parsed-spec))
                  (mapcat (partial property-type-walk
                                   (partial gen-type-spec-2 primitive-type-mapping))))
            (select-keys (:PropertyTypes parsed-spec)
                         (root-property-types parsed-spec))))

(defn gen-resource-type-specs-2
  "Generates all of the specs for ResourceTypes"
  [parsed-spec primitive-type-mapping]
  (mapcat (partial gen-type-spec-2 primitive-type-mapping) (:ResourceTypes parsed-spec)))

(defn gen-specs-2
  "Generates specs for all PropertyTypes and ResourceTypes allowing the user to
  set which primitive type mapping to use"
  [parsed-spec primitive-type-mapping]
  (doall (concat (gen-property-type-specs-2 parsed-spec primitive-type-mapping)
                 (gen-resource-type-specs-2 parsed-spec primitive-type-mapping))))
