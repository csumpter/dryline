(ns roomkey.dryline.template
  (:require [roomkey.dryline.parse :as parse]
            [roomkey.dryline.keywords :as kws]
            [roomkey.dryline.specs :as specs]
            [roomkey.dryline.simple-intrinsic-function :as sif]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]))

(s/def ::alphanumeric-string (s/and string? #(re-matches #"[A-Za-z0-9]+" %)))

;; https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/template-anatomy.html
(s/def ::AWSTemplateFormatVersion #{"2010-09-09"}) ; optional

(s/def ::Description (s/and string? #(< 0 (count %) 1024))) ; optional

(s/def ::Metadata some?) ;; json

(s/def ::Parameters some?)

(s/def ::Mappings (s/map-of ::alphanumeric-string
                            (s/map-of string?
                                      (s/map-of ::alphanumeric-string
                                                (s/or :string string?
                                                      :list (s/coll-of string?))))))

(s/def ::Conditions (s/map-of ::alphanumeric-string
                              ::sif/simple-intrinsic-function))

(s/def ::Transform (s/or :string string?
                         :list (s/coll-of string?)))



;; Resources
(defmulti resource-type :Type)

(defn add-method-to-resource-type
  [resource-type-identifier]
  (let [resource-type-spec (kws/resource-type-keyword resource-type-identifier)
        properties-spec (kws/resource-type-property-keyword resource-type-identifier
                                                            :Properties)]
    #_(when (get (s/registry) properties-spec)
        (throw (ex-info "Spec of the same name already exists"
                        {:spec properties-spec
                         :description (s/describe properties-spec)})))
    (eval `(s/def ~properties-spec ~resource-type-spec))
    (eval `(defmethod resource-type ~resource-type-identifier [_#]
             (s/keys :req-un [~properties-spec])))))

(s/def ::resource-type (s/multi-spec resource-type :Type))

(s/def ::Resources (s/map-of ::alphanumeric-string ::resource-type)) ; required

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

(defn encode
  [template]
  (when-not (s/valid? ::template template)
    (throw (ex-info "template did not conform to spec"
                    (s/explain-data ::template template))))
  (json/generate-string template))

(comment

  (def parsed-spec (-> "aws/CloudFormationResourceSpecification.json"
                       io/resource
                       io/reader
                       parse/parse))

  (specs/add-specs parsed-spec sif/primitive-type->spec)
  (doseq [rti (keys (:ResourceTypes parsed-spec))]
    (add-method-to-resource-type rti))

  (def bucket {:Type "AWS::S3::Bucket"
               :Properties {:BucketName {"Fn::Get" "abc"}}})

  (def queue {:Type "AWS::SQS::Queue"
              :Properties {}})


  (def template
    {:AWSTemplateFormatVersion "2010-09-09"
     :Resources {"Bucket" bucket
                 "Queue" queue}})


  (s/valid? ::template template)

  (s/explain ::template template)
  )

