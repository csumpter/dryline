(ns roomkey.dryline.template
  (:require [roomkey.dryline.keywords :as kws]
            [roomkey.dryline.simple-intrinsic-function :as sif]
            [clojure.spec.alpha :as s]))

;; These are hand written specs based on the description of a valid
;; CloudFormation template found at:
;; https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/template-anatomy.html

(s/def ::logical-id (s/and string? #(re-matches #"[A-Za-z0-9]+" %)))

(s/def ::literal
  (s/or :string string?
        :integer int?
        :double double?
        :boolean boolean?
        :timestamp inst?))

(s/def ::literal-json
  (s/or :string string?
        :integer int?
        :double double?
        :boolean boolean?
        :vector (s/coll-of ::literal-json :kind vector?)
        :map (s/map-of string? ::literal-json)))

(s/def ::AWSTemplateFormatVersion #{"2010-09-09"})

(s/def ::Description (s/and string? #(< 0 (count %) 1024)))

(s/def ::Metadata ::literal-json)

(s/def :roomkey.dryline.template.parameters/Type string?)
(s/def ::Parameters
  (s/map-of ::logical-id
            (s/keys :req-un [:roomkey.dryline.template.parameters/Type])))

(s/def ::Mappings (s/map-of ::logical-id
                            (s/map-of string?
                                      (s/map-of ::alphanumeric-string
                                                (s/or :string string?
                                                      :list (s/coll-of string?))))))

(s/def ::Conditions (s/map-of ::logical-id
                              ::sif/simple-intrinsic-function))

(s/def ::Transform (s/or :string string?
                         :list (s/coll-of string?)))

;; Resources
(defmulti resource-type :Type)

(defmethod resource-type :default [rti]
  (let [message (format (str "Warning! Using an unregistered resource-type: %s. "
                             "The resource will not be validated.")
                        (:Type rti))]
    (println message))
  map?)

(defn- properties-keyword
  "Returns a keyword for the :Properties key of a resource-type or the form
  :roomkey.<serviceprovider>.resource-type.<servicename>.<ResourceTypeName>/Properties"
  [resource-type-identifier]
  (let [[provider service resource-type]
        (kws/split-type-identifier resource-type-identifier)]
    (keyword (kws/make-namespace provider "resource-type" service resource-type)
             "Properties")))

(defn- add-method-to-resource-type
  "Registers a properties spec for the resource type identifier. An exception
  will be thrown if specs for the resource type have not been registered."
  [resource-type-identifier]
  (let [resource-type-spec (kws/resource-type-keyword resource-type-identifier)
        properties-spec (properties-keyword resource-type-identifier)]
    (eval `(s/def ~properties-spec ~resource-type-spec))
    (eval `(defmethod resource-type ~resource-type-identifier [_#]
             (s/keys :req-un [~properties-spec])))))

(defn add-methods-to-resource-type
  "Registers a properties spec for each of the resource type identifiers. An
  exception will be thrown if specs for a resource type have not been
  registered."
  [resource-type-identifiers]
  (doseq [rti resource-type-identifiers]
    (add-method-to-resource-type rti)))

(s/def ::resource-type (s/multi-spec resource-type :Type))

(s/def ::Resources (s/map-of ::alphanumeric-string ::resource-type))

;; Outputs
(s/def :roomkey.dryline.template.outputs/Description
  (s/and string? #(< 0 (count %) 1024)))
(s/def :roomkey.dryline.template.outputs/Value some?)
(s/def :roomkey.dryline.template.outputs/Export some?)
(s/def ::Outputs
  (s/map-of ::alphanumeric-string
            (s/keys :req-un [:roomkey.dryline.template.outputs/Value]
                    :opt-un [:roomkey.dryline.template.outputs/Description
                             :roomkey.dryline.template.outputs/Export])))

(s/def ::template
  (s/keys :req-un [::Resources]
          :opt-un [::AWSTemplateFormatVersion
                   ::Description
                   ::Metadata
                   ::Parameters
                   ::Mappings
                   ::Conditions
                   ::Transform
                   ::Outputs]))
