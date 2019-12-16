(ns roomkey.dryline.simple-intrinsic-function
  (:require [clojure.spec.alpha :as s]
            [roomkey.dryline.specs :as specs]))

(s/def :roomkey.aws.cloudformation/simple-intrinsic-function
  (s/map-of (s/or :ref #{"Ref"} :fn (s/and string? #(clojure.string/starts-with? % "Fn::")))
            any?))

(def primitive-type->predicate
  "A map from CF PrimitiveType to clojure predicates"
  {"String" '(clojure.spec.alpha/or
              :primitive string?
              :simple-intrinsic-function :roomkey.aws.cloudformation/simple-intrinsic-function)
   "Long" '(clojure.spec.alpha/or
            :primitive int?
            :simple-intrinsic-function :roomkey.aws.cloudformation/simple-intrinsic-function)
   "Integer" '(clojure.spec.alpha/or
               :primitive int?
               :simple-intrinsic-function :roomkey.aws.cloudformation/simple-intrinsic-function)
   "Double" '(clojure.spec.alpha/or
              :primitive double?
              :simple-intrinsic-function :roomkey.aws.cloudformation/simple-intrinsic-function)
   "Boolean" '(clojure.spec.alpha/or
               :primitive boolean?
               :simple-intrinsic-function :roomkey.aws.cloudformation/simple-intrinsic-function)
   "Timestamp" '(clojure.spec.alpha/or
                 :primitive inst?
                 :simple-intrinsic-function :roomkey.aws.cloudformation/simple-intrinsic-function)
   "Json" '(clojure.spec.alpha/or
            :primitive ::specs/json
            :simple-intrinsic-function :roomkey.aws.cloudformation/simple-intrinsic-function)})
