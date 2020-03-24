(ns roomkey.dryline
  (:require [roomkey.dryline.specs :as specs]
            [roomkey.dryline.parse :as parse]
            [roomkey.dryline.validation]
            [clojure.spec.alpha :as s]))

(defn parse-specification-and-add-specs
  "Parses the supplied specification as a reader and adds all specs to the
  registry. If :validate is true the specification file will be validated
  before adding specs and will throw an exception if invalid"
  [rdr primitive-type-mapping & {:keys [validate]}]
  (let [parsed-spec (parse/parse rdr)]
    (when validate
      (when-some [data (s/explain-data :roomkey.aws.cloudformation/Specification
                                       parsed-spec)]
        (throw (ex-info "Specification file is not valid" data))))
    (specs/add-specs parsed-spec primitive-type-mapping)))

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
