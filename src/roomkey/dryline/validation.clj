(ns roomkey.dryline.validation
  (:require [roomkey.dryline.parse :as parse]
            [clojure.spec.alpha :as s]))

(def documentation-url-regex
  #"^((http[s]?):\/\/)(docs.aws.amazon.com\/)([^:\/\s]+)((\/\w+)*\/)([\w\-\.]+[^#?\s]+)(.*)?(#[\w\-]+)?$")

(def primitive-types #{"String" "Long" "Integer" "Double" "Boolean" "Timestamp" "Json"})

;; PropertyTypes
(s/def :roomkey.aws.cloudformation.propertytype/Documentation
  (s/and string?
         #(re-matches documentation-url-regex %)))

(s/def :roomkey.aws.cloudformation.propertytype/DuplicatesAllowed boolean?)

(s/def :roomkey.aws.cloudformation.propertytype/ItemType string?)

(s/def :roomkey.aws.cloudformation.propertytype/PrimitiveItemType primitive-types)

(s/def :roomkey.aws.cloudformation.propertytype/PrimitiveItem primitive-types)

(s/def :roomkey.aws.cloudformation.propertytype/Required boolean?)

(s/def :roomkey.aws.cloudformation.propertytype/Type string?)

(s/def :roomkey.aws.cloudformation.propertytype/UpdateType
  #{"Mutable" "Immutable" "Conditional"})

(s/def :roomkey.aws.cloudformation/PropertySpecification
  (s/keys :opt-un [:roomkey.aws.cloudformation.propertytype/Documentation
                   :roomkey.aws.cloudformation.propertytype/DuplicatesAllowed
                   :roomkey.aws.cloudformation.propertytype/ItemType
                   :roomkey.aws.cloudformation.propertytype/PrimitiveItemType
                   :roomkey.aws.cloudformation.propertytype/PrimitiveItem
                   :roomkey.aws.cloudformation.propertytype/Required
                   :roomkey.aws.cloudformation.propertytype/Type
                   :roomkey.aws.cloudformation.propertytype/UpdateType]))

(s/def :roomkey.aws.cloudformation/Properties
  (s/map-of keyword? :roomkey.aws.cloudformation/PropertySpecification))

(s/def :roomkey.aws.cloudformation/PropertyType
  (s/keys :req-un [:roomkey.aws.cloudformation.propertytype/Documentation
                   :roomkey.aws.cloudformation/Properties]))

(s/def :roomkey.aws.cloudformation/PropertyTypes
  (s/map-of string?
            (s/or :property-type :roomkey.aws.cloudformation/PropertyType
                  :property :roomkey.aws.cloudformation/PropertySpecification)))

;; ResourceTypes
(s/def :roomkey.aws.cloudformation.resourcetype.attribute/ItemType string?)

(s/def :roomkey.aws.cloudformation.resourcetype.attribute/PrimitiveItemType string?)

(s/def :roomkey.aws.cloudformation.resourcetype.attribute/PrimitiveType primitive-types)

(s/def :roomkey.aws.cloudformation.resourcetype.attribute/Type string?)

(s/def :roomkey.aws.cloudformation.resourcetype/Attribute
  (s/keys :req-un []
          :opt-un [:roomkey.aws.cloudformation.resourcetype.attribute/ItemType
                   :roomkey.aws.cloudformation.resourcetype.attribute/PrimitiveItemType
                   :roomkey.aws.cloudformation.resourcetype.attribute/PrimitiveType
                   :roomkey.aws.cloudformation.resourcetype.attribute/Type]))

(s/def :roomkey.aws.cloudformation.resourcetype/AttributeName keyword?)

(s/def :roomkey.aws.cloudformation.resourcetype/Attributes
  (s/map-of :roomkey.aws.cloudformation.resourcetype/AttributeName
            :roomkey.aws.cloudformation.resourcetype/Attribute))

(s/def :roomkey.aws.cloudformation.resourcetype/Documentation string?)

(s/def :roomkey.aws.cloudformation.resourcetype/Properties
  (s/map-of keyword? :roomkey.aws.cloudformation/PropertySpecification))

(s/def :roomkey.aws.cloudformation/ResourceType
  (s/keys :req-un [:roomkey.aws.cloudformation.resourcetype/Documentation
                   :roomkey.aws.cloudformation.resourcetype/Properties]
          :opt-un [:roomkey.aws.cloudformation.resourcetype/Attributes]))

(s/def :roomkey.aws.cloudformation/ResourceTypes
  (s/map-of string?
            :roomkey.aws.cloudformation/ResourceType))

;; ResourceSpecificationVersion
(s/def :roomkey.aws.cloudformation/ResourceSpecificationVersion
  (s/and string? #(re-matches #"^([\d]+[.]?)+$" %)))

;; Specification
(s/def :roomkey.aws.cloudformation/Specification
  (s/keys :req-un [:roomkey.aws.cloudformation/ResourceTypes
                   :roomkey.aws.cloudformation/ResourceSpecificationVersion]
          :opt-un [:roomkey.aws.cloudformation/PropertyTypes]))

(defn validate
  [rdr]
  (s/explain :roomkey.aws.cloudformation/Specification (parse/parse rdr)))
