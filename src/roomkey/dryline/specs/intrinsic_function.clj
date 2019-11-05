(ns roomkey.dryline.specs.intrinsic-function
  (:require [clojure.spec.alpha :as s]))

(s/def :roomkey.aws.cloudformation.intrinsic-function/Ref
  (s/map-of #{"Ref"} (s/or :string string? :keyword keyword?)))

(s/def :roomkey.aws.cloudformation.intrinsic-function/Base64
  (s/map-of #{"Fn::Base64"} string?))

(s/def :roomkey.aws.cloudformation.intrinsic-function.Cidr/ipBlock
  (s/or :select :roomkey.aws.cloudformation.intrinsic-function/Select
        :ref :roomkey.aws.cloudformation.intrinsic-function/Ref
        :string string?))
(s/def :roomkey.aws.cloudformation.intrinsic-function.Cidr/count (s/int-in 1 257))
(s/def :roomkey.aws.cloudformation.intrinsic-function.Cidr/cidrBits int?)
(s/def :roomkey.aws.cloudformation.intrinsic-function.Cidr/tuple
  (s/tuple :roomkey.aws.cloudformation.intrinsic-function.Cidr/ipBlock
           :roomkey.aws.cloudformation.intrinsic-function.Cidr/count
           :roomkey.aws.cloudformation.intrinsic-function.Cidr/cidrBits))
(s/def :roomkey.aws.cloudformation.intrinsic-function/Cidr
  (s/map-of #{"Fn::Cidr"} :roomkey.aws.cloudformation.intrinsic-function.Cidr/tuple))

(s/def :roomkey.aws.cloudformation.intrinsic-function.FindInMap/value
  (s/or :find-in-map :roomkey.aws.cloudformation.intrinsic-function/FindInMap
        :ref :roomkey.aws.cloudformation.intrinsic-function/Ref
        :string string?))

(s/def :roomkey.aws.cloudformation.intrinsic-function/FindInMap
  (s/map-of #{"Fn::FindInMap"}
            (s/tuple :roomkey.aws.cloudformation.intrinsic-function.FindInMap/value
                     :roomkey.aws.cloudformation.intrinsic-function.FindInMap/value
                     :roomkey.aws.cloudformation.intrinsic-function.FindInMap/value)))

(s/def :roomkey.aws.cloudformation.intrinsic-function/GetAtt
  (s/map-of #{"Fn::GetAtt"}
            (s/tuple string?
                     (s/or :ref :roomkey.aws.cloudformation.intrinsic-function/Ref
                           :string string?))))

(s/def :roomkey.aws.cloudformation.intrinsic-function/GetAZs
  (s/map-of #{"Fn::GetAtt"}
            (s/or :ref :roomkey.aws.cloudformation.intrinsic-function/Ref
                  :string string?)))

(s/def :roomkey.aws.cloudformation.intrinsic-function/ImportValue
  (s/map-of #{"Fn::GetAtt"}
            (s/or :string string?
                  :base64 :roomkey.aws.cloudformation.intrinsic-function/Base64
                  :find-in-map :roomkey.aws.cloudformation.intrinsic-function/FindInMap
                  :join :roomkey.aws.cloudformation.intrinsic-function/Join
                  :select :roomkey.aws.cloudformation.intrinsic-function/Select
                  :split :roomkey.aws.cloudformation.intrinsic-function/Split
                  :sub :roomkey.aws.cloudformation.intrinsic-function/Sub
                  :ref :roomkey.aws.cloudformation.intrinsic-function/Ref)))

(s/def :roomkey.aws.cloudformation.intrinsic-function/Join
  (s/map-of #{"Fn::Join"} (s/tuple string? (s/coll-of string?))))

(s/def :roomkey.aws.cloudformation.intrinsic-function/Select
  (s/map-of #{"Fn::Select"} (s/tuple int? (s/coll-of some?))))

(s/def :roomkey.aws.cloudformation.intrinsic-function/Split
  (s/map-of #{"Fn::Split"} (s/tuple string? string?)))

(s/def :roomkey.aws.cloudformation.intrinsic-function/Sub
  (s/map-of #{"Fn::Sub"} (s/or :short string?
                               :full (s/tuple string?
                                              (s/map-of string? string?)))))

(s/def :roomkey.aws.cloudformation.intrinsic-function.Transform/Name string?)
(s/def :roomkey.aws.cloudformation.intrinsic-function.Transform/Parameters
  (s/map-of string? string?))

(s/def :roomkey.aws.cloudformation.intrinsic-function/Transform
  (s/map-of #{"Fn::Transform"}
            (s/keys :req-un [:roomkey.aws.cloudformation.intrinsic-function.Transform/Name
                             :roomkey.aws.cloudformation.intrinsic-function.Transform/Parameters])))

(defmulti intrinsic-function (comp key first))

(defmethod intrinsic-function "Fn::Base64" [_]
  :roomkey.aws.cloudformation.intrinsic-function/Base64)

(defmethod intrinsic-function "Fn::Cidr" [_]
  :roomkey.aws.cloudformation.intrinsic-function/Cidr)

(defmethod intrinsic-function "Fn::FindInMap" [_]
  :roomkey.aws.cloudformation.intrinsic-function/FindInMap)

(defmethod intrinsic-function "Fn::GetAtt" [_]
  :roomkey.aws.cloudformation.intrinsic-function/GetAtt)

(defmethod intrinsic-function "Fn::GetAZs" [_]
  :roomkey.aws.cloudformation.intrinsic-function/GetAZs)

(defmethod intrinsic-function "Fn::ImportValues" [_]
  :roomkey.aws.cloudformation.intrinsic-function/ImportValues)

(defmethod intrinsic-function "Fn::Join" [_]
  :roomkey.aws.cloudformation.intrinsic-function/Join)

(defmethod intrinsic-function "Fn::Split" [_]
  :roomkey.aws.cloudformation.intrinsic-function/Split)

(defmethod intrinsic-function "Fn::Sub" [_]
  :roomkey.aws.cloudformation.intrinsic-function/Sub)

(defmethod intrinsic-function "Fn::Transform" [_]
  :roomkey.aws.cloudformation.intrinsic-function/Transform)

(defmethod intrinsic-function "Ref" [_]
  :roomkey.aws.cloudformation.intrinsic-function/Ref)

(s/def :roomkey.aws.cloudformation/intrinsic-function
  (s/multi-spec intrinsic-function first))

(def ^:private primitive-type->predicate
  "A map from CF PrimitiveType to clojure predicates"
  {"String" (s/or :string string?
                  :intrinsic-function :roomkey.aws.cloudformation/intrinsic-function)
   "Long" (s/or :string int?
                :intrinsic-function :roomkey.aws.cloudformation/intrinsic-function)
   "Integer" (s/or :string int?
                   :intrinsic-function :roomkey.aws.cloudformation/intrinsic-function)
   "Double" (s/or :string double?
                  :intrinsic-function :roomkey.aws.cloudformation/intrinsic-function)
   "Boolean" (s/or :string boolean?
                   :intrinsic-function :roomkey.aws.cloudformation/intrinsic-function)
   "Timestamp" (s/or :string inst?
                     :intrinsic-function :roomkey.aws.cloudformation/intrinsic-function)
   "Json" (s/or :string map?
                :intrinsic-function :roomkey.aws.cloudformation/intrinsic-function)})

