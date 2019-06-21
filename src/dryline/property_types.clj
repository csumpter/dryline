(ns dryline.property-types
  (:require [clojure.spec.alpha :as s]
            [cheshire.core :as json]
            [clojure.string :as string]))

;(s/def ::Primitive #{"String" "Long" "Integer" "Double" "Boolean" "Timestamp"})
;(s/def ::NonPrimitive (s/or :str string? :key keyword?))
;(s/def ::Documentation string?)
;(s/def ::DuplicatesAllowed boolean?)
;(s/def ::ItemType (s/or :map (s/map-of string? ::NonPrimitive)
;                        :vec (s/every ::NonPrimitive)))
;(s/def ::PrimitiveItemType (s/or :map (s/map-of string? ::Primitive)
;                                 :vec (s/every ::Primitive)))
;(s/def ::PrimitiveType ::Primitive)
;(s/def ::Required boolean?)
;(s/def ::Type ::NonPrimitive)
;(s/def ::UpdateType #{"Mutable" "Immutable" "Conditional"})
;
;(s/def ::Property (s/keys :req-un [::Documentation
;                                   ::DuplicatesAllowed
;                                   ::Required
;                                   ::UpdateType]
;                          :opt-un [::ItemType
;                                   ::PrimitiveItemType
;                                   ::PrimitiveType
                                        ;                                   ::Type]))

(s/def ::Documentation string?)
(s/def ::DuplicatesAllowed boolean?)
(s/def ::Required boolean?)
(s/def ::UpdateType #{"Mutable" "Immutable" "Conditional"})
(s/def ::Type (s/or :const #{"List" "Map"}
                    :prop-name string? ;; TODO fix to allow recursive property names
                    ))
(s/def ::PrimitiveType #{"String" "Long" "Integer" "Double" "Boolean" "Timestamp" "Json"})
(s/def ::PrimitiveItemType #{"String" "Long" "Integer" "Double" "Boolean" "Timestamp" "Json"}
  ;; TODO validate Type is list or map
  )
(s/def ::ItemType string?
  ;; TODO fix to allow recursive property names
  )
(s/def ::PropertyDefinition (s/keys :opt-un [::Documentation
                                             ::Required
                                             ::UpdateType
                                             ::DuplicatesAllowed
                                             ::Type
                                             ::PrimitiveType
                                             ::PrimitiveItemType
                                             ::ItemType]))

(s/def ::Properties (s/map-of keyword? ::PropertyDefinition))

(s/def ::PropertyType (s/or :sub-props (s/keys :req-un [::Properties]
                                               :opt-un [::Documentation])
                            :prop ::PropertyDefinition))

(s/def ::PropertyList (s/map-of keyword? ::PropertyType))

(def aws-spec (json/parse-string (slurp "resources/aws/us-east-spec.json")
                                 (fn [k] (-> k
                                             (string/replace #"(.*)::" "$1/")
                                             (string/replace #"::" ".")
                                             keyword))))

(def props (:PropertyTypes aws-spec))

(s/explain-data ::PropertyList props)

