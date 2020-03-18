(ns roomkey.dryline
  (:require [roomkey.dryline.specs :as specs]
            [roomkey.dryline.parse :as parse]))

(defn parse-spefictiaction-and-add-specs
  "Parses the supplied specification as a reader and adds all specs to the
  registry"
  [rdr primitive-type-mapping]
  (specs/gen-specs (parse/parse rdr) primitive-type-mapping))
