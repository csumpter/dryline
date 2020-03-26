(ns roomkey.dryline.simple-intrinsic-function
  (:require [clojure.spec.alpha :as s]
            [clojure.string]))

(s/def ::simple-intrinsic-function
  (s/map-of (s/or :ref #{"Ref"} :fn (s/and string? #(clojure.string/starts-with? % "Fn::")))
            any?))

(s/def ::string
  (clojure.spec.alpha/or
   :primitive string?
   :simple-intrinsic-function ::simple-intrinsic-function))

(s/def ::integer
  (clojure.spec.alpha/or
   :primitive int?
   :simple-intrinsic-function ::simple-intrinsic-function))

(s/def ::double
  (clojure.spec.alpha/or
   :primitive double?
   :simple-intrinsic-function ::simple-intrinsic-function))

(s/def ::boolean
  (clojure.spec.alpha/or
   :primitive boolean?
   :simple-intrinsic-function ::simple-intrinsic-function))

(s/def ::timestamp
  (clojure.spec.alpha/or
   :primitive inst?
   :simple-intrinsic-function ::simple-intrinsic-function))

(s/def ::json
  (s/or :string ::string
        :integer ::integer
        :double ::double
        :boolean ::boolean
        :vector (s/coll-of ::json :kind vector?)
        :map (s/map-of string? ::json)))

(def primitive-type->spec
  "A map from CF PrimitiveType to clojure predicates"
  {"String" ::string
   "Long" ::integer
   "Integer" ::integer
   "Double" ::double
   "Boolean" ::boolean
   "Timestamp" ::timestamp
   "Json" ::json})

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
