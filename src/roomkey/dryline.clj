(ns roomkey.dryline
  (:require [roomkey.dryline.specs :as specs]
            [roomkey.dryline.parse :as parse]
            [roomkey.dryline.template :as template]
            [roomkey.dryline.validation :as validation]
            [cheshire.core :as json]
            [clojure.spec.alpha :as s]))

(defn parse-specification-and-add-specs
  "Parses the supplied specification as a reader and adds all specs to the
  registry. If :validate is true the specification file will be validated
  before adding specs and will throw an exception if invalid"
  [rdr primitive-type-mapping & {:keys [validate]}]
  (let [parsed-spec (parse/parse rdr)]
    (when validate
      (when-some [data (s/explain-data ::validation/Specification
                                       parsed-spec)]
        (throw (ex-info "Specification file is not valid" data))))
    (specs/add-specs parsed-spec primitive-type-mapping)
    (template/add-methods-to-resource-type (keys (:ResourceTypes parsed-spec)))))

(defn encode
  "Encodes template as a JSON string. If :validate is true, template will be
  validated against the :roomkey.dryline.template/template spec. If template is
  not valid an exception will be thrown."
  [template & {:keys [validate]}]
  (when validate
    (when-some [data (s/explain-data ::template/template template)]
      (throw (ex-info "template did not conform to spec" data))))
  (json/generate-string template))
