(ns roomkey.dryline.simple-intrinsic-function
  (:require [clojure.spec.alpha :as s]
            [clojure.string]))

(s/def ::simple-intrinsic-function
  (s/map-of (s/or :ref #{"Ref"} :fn (s/and string? #(clojure.string/starts-with? % "Fn::")))
            any?))

(s/def ::string
  (s/or :primitive string?
        :simple-intrinsic-function ::simple-intrinsic-function))

(s/def ::integer
  (s/or :primitive int?
        :simple-intrinsic-function ::simple-intrinsic-function))

(s/def ::double
  (s/or :primitive double?
        :simple-intrinsic-function ::simple-intrinsic-function))

(s/def ::boolean
  (s/or :primitive boolean?
        :simple-intrinsic-function ::simple-intrinsic-function))

(s/def ::timestamp
  (s/or :primitive inst?
        :simple-intrinsic-function ::simple-intrinsic-function))

(s/def ::unqualified-keyword
  (s/and keyword? (complement qualified-keyword?)))

(s/def ::json
  (s/or :string ::string
        :integer ::integer
        :double ::double
        :boolean ::boolean
        :vector (s/coll-of ::json :kind vector?)
        :map (s/map-of (s/or :string string?
                             :unqualified-keyword ::unqualified-keyword)
                       ::json)))

(def primitive-type->spec
  "A map from CF PrimitiveType to clojure predicates"
  {"String" ::string
   "Long" ::integer
   "Integer" ::integer
   "Double" ::double
   "Boolean" ::boolean
   "Timestamp" ::timestamp
   "Json" ::json})
