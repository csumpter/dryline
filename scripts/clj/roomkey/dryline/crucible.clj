(ns roomkey.dryline.crucible
  (:require [roomkey.dryline.parse :as parse]
            [roomkey.dryline.specs :as specs]
            [roomkey.dryline.specs.intrinsic-function :as int-fn]
            [clojure.java.io :as io]
            [crucible.resources]))

(def parsed-spec (parse/parse (io/reader (io/resource "aws/us-east-spec.json"))))

(specs/gen-specs parsed-spec int-fn/primitive-type->predicate-or-simple-intrinsic-funtion)

(defn get-resource-type-spec-kws
  [parsed-spec]
  (map specs/dryline-keyword (keys (:ResourceTypes parsed-spec))))

(defn create-crucible-resource-from-spec
  [spec-kw]
  (binding [*ns* (create-ns (symbol (namespace spec-kw)))]
    (eval `(crucible.resources/defresource ~(symbol spec-kw)
             ~(get-in @specs/spec-metadata [spec-kw :ResourceTypeName])
             ~spec-kw))))

(doseq [spec (get-resource-type-spec-kws parsed-spec)]
  (create-crucible-resource-from-spec spec))

