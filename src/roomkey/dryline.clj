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
