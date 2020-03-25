(ns roomkey.dryline.validation
  (:require [roomkey.dryline.parse :as parse]
            [clojure.spec.alpha :as s]))

(def documentation-url-regex
  #"^((http[s]?):\/\/)(docs.aws.amazon.com\/)([^:\/\s]+)((\/\w+)*\/)([\w\-\.]+[^#?\s]+)(.*)?(#[\w\-]+)?$")

(def primitive-types #{"String" "Long" "Integer" "Double" "Boolean" "Timestamp" "Json"})

;; PropertyTypes
(s/def :roomkey.dryline.validation.propertytype/Documentation
  (s/and string?
         #(re-matches documentation-url-regex %)))

(s/def :roomkey.dryline.validation.propertytype/DuplicatesAllowed boolean?)

(s/def :roomkey.dryline.validation.propertytype/ItemType string?)

(s/def :roomkey.dryline.validation.propertytype/PrimitiveItemType primitive-types)

(s/def :roomkey.dryline.validation.propertytype/PrimitiveItem primitive-types)

(s/def :roomkey.dryline.validation.propertytype/Required boolean?)

(s/def :roomkey.dryline.validation.propertytype/Type string?)

(s/def :roomkey.dryline.validation.propertytype/UpdateType
  #{"Mutable" "Immutable" "Conditional"})

(s/def :roomkey.dryline.validation/PropertySpecification
  (s/keys :opt-un [:roomkey.dryline.validation.propertytype/Documentation
                   :roomkey.dryline.validation.propertytype/DuplicatesAllowed
                   :roomkey.dryline.validation.propertytype/ItemType
                   :roomkey.dryline.validation.propertytype/PrimitiveItemType
                   :roomkey.dryline.validation.propertytype/PrimitiveItem
                   :roomkey.dryline.validation.propertytype/Required
                   :roomkey.dryline.validation.propertytype/Type
                   :roomkey.dryline.validation.propertytype/UpdateType]))

(s/def :roomkey.dryline.validation/Properties
  (s/map-of keyword? :roomkey.dryline.validation/PropertySpecification))

(s/def :roomkey.dryline.validation/PropertyType
  (s/keys :req-un [:roomkey.dryline.validation.propertytype/Documentation
                   :roomkey.dryline.validation/Properties]))

(s/def :roomkey.dryline.validation/PropertyTypes
  (s/map-of string?
            (s/or :property-type :roomkey.dryline.validation/PropertyType
                  :property :roomkey.dryline.validation/PropertySpecification)))

;; ResourceTypes
(s/def :roomkey.dryline.validation.resourcetype.attribute/ItemType string?)

(s/def :roomkey.dryline.validation.resourcetype.attribute/PrimitiveItemType string?)

(s/def :roomkey.dryline.validation.resourcetype.attribute/PrimitiveType primitive-types)

(s/def :roomkey.dryline.validation.resourcetype.attribute/Type string?)

(s/def :roomkey.dryline.validation.resourcetype/Attribute
  (s/keys :req-un []
          :opt-un [:roomkey.dryline.validation.resourcetype.attribute/ItemType
                   :roomkey.dryline.validation.resourcetype.attribute/PrimitiveItemType
                   :roomkey.dryline.validation.resourcetype.attribute/PrimitiveType
                   :roomkey.dryline.validation.resourcetype.attribute/Type]))

(s/def :roomkey.dryline.validation.resourcetype/AttributeName keyword?)

(s/def :roomkey.dryline.validation.resourcetype/Attributes
  (s/map-of :roomkey.dryline.validation.resourcetype/AttributeName
            :roomkey.dryline.validation.resourcetype/Attribute))

(s/def :roomkey.dryline.validation.resourcetype/Documentation string?)

(s/def :roomkey.dryline.validation.resourcetype/Properties
  (s/map-of keyword? :roomkey.dryline.validation/PropertySpecification))

(s/def :roomkey.dryline.validation/ResourceType
  (s/keys :req-un [:roomkey.dryline.validation.resourcetype/Documentation
                   :roomkey.dryline.validation.resourcetype/Properties]
          :opt-un [:roomkey.dryline.validation.resourcetype/Attributes]))

(s/def :roomkey.dryline.validation/ResourceTypes
  (s/map-of string?
            :roomkey.dryline.validation/ResourceType))

;; ResourceSpecificationVersion
(s/def :roomkey.dryline.validation/ResourceSpecificationVersion
  (s/and string? #(re-matches #"^([\d]+[.]?)+$" %)))

;; Specification
(s/def :roomkey.dryline.validation/Specification
  (s/keys :req-un [:roomkey.dryline.validation/ResourceTypes
                   :roomkey.dryline.validation/ResourceSpecificationVersion]
          :opt-un [:roomkey.dryline.validation/PropertyTypes]))

(defn validate
  [rdr]
  (s/explain :roomkey.dryline.validation/Specification (parse/parse rdr)))
