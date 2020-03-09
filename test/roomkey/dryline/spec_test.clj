(ns roomkey.dryline.spec-test
  (:require [clojure.java.io :as io]
            [clojure.test :as t]
            [roomkey.dryline.specs :as specs]
            [roomkey.dryline.parse :as parse]
            [clojure.spec.alpha :as s]))

(t/deftest ^:unit dryline-keyword
  (let [sut specs/dryline-keyword]
    (t/are [type-name kw] (= (sut type-name) kw)
      "AWS::S3::Bucket.ObjectLockConfiguration"
      :roomkey.aws.s3.Bucket.ObjectLockConfiguration/ObjectLockConfiguration

      "AWS::S3::Bucket"
      :roomkey.aws.s3/Bucket

      "Tag"
      :roomkey.aws/Tag)))

(t/deftest ^:unit resource-type-keyword
  (let [sut #'specs/resource-type-keyword]
    (t/is (= (sut "AWS::WAFv2::WebACL")
             :roomkey.aws.wafv2/WebACL))))

(t/deftest ^:unit resource-type-property-keyword
  (let [sut #'specs/resource-type-property-keyword]
    (t/is (= (sut "AWS::WAFv2::WebACL" :Rules)
             :roomkey.aws.wafv2.WebACL/Rules))))

(t/deftest ^:unit property-type-keyword
  (let [sut #'specs/property-type-keyword]
    (t/is (= (sut "AWS::WAFv2::WebACL.Rules")
             :roomkey.aws.wafv2.WebACL.properties/Rules))))

(t/deftest ^:unit property-type-property-keyword
  (let [sut #'specs/property-type-property-keyword]
    (t/is (= (sut "AWS::WAFv2::WebACL.Rules" :Rules)
             :roomkey.aws.wafv2.WebACL.properties.Rules/Rules))))

(t/deftest ^:unit referenced-property-type-spec
  (let [sut #'specs/referenced-property-type-spec]
    (t/is (= (sut "AWS::WAFv2::WebACL.Rules" :Rule)
             :roomkey.aws.wafv2.WebACL.properties/Rule))))


(t/deftest ^:unit s3-specs
  (let [parsed-spec (-> "aws/S3BucketSpecification.json"
                        io/resource
                        io/reader
                        parse/parse)]
    (specs/gen-specs parsed-spec specs/primitive-type->predicate)
    (t/is (= (s/describe :roomkey.aws.s3/Bucket)
             '(keys
               :opt-un
               [:roomkey.aws.s3.Bucket/AccessControl
                :roomkey.aws.s3.Bucket/AnalyticsConfigurations
                :roomkey.aws.s3.Bucket/AccelerateConfiguration
                :roomkey.aws.s3.Bucket/VersioningConfiguration
                :roomkey.aws.s3.Bucket/Tags
                :roomkey.aws.s3.Bucket/ObjectLockEnabled
                :roomkey.aws.s3.Bucket/NotificationConfiguration
                :roomkey.aws.s3.Bucket/MetricsConfigurations
                :roomkey.aws.s3.Bucket/PublicAccessBlockConfiguration
                :roomkey.aws.s3.Bucket/InventoryConfigurations
                :roomkey.aws.s3.Bucket/CorsConfiguration
                :roomkey.aws.s3.Bucket/ObjectLockConfiguration
                :roomkey.aws.s3.Bucket/LifecycleConfiguration
                :roomkey.aws.s3.Bucket/ReplicationConfiguration
                :roomkey.aws.s3.Bucket/LoggingConfiguration
                :roomkey.aws.s3.Bucket/BucketName
                :roomkey.aws.s3.Bucket/BucketEncryption
                :roomkey.aws.s3.Bucket/WebsiteConfiguration])))
    (t/is (= (get (s/registry) :roomkey.aws.s3.Bucket/ObjectLockConfiguration)
             :roomkey.aws.s3.Bucket.properties/ObjectLockConfiguration))
    (t/is (= (s/describe :roomkey.aws.s3.Bucket.properties/ObjectLockConfiguration)
             '(keys
               :opt-un
               [:roomkey.aws.s3.Bucket.properties.ObjectLockConfiguration/ObjectLockEnabled
                :roomkey.aws.s3.Bucket.properties.ObjectLockConfiguration/Rule])))
    (t/is (= (get (s/registry) :roomkey.aws.s3.Bucket.properties.ObjectLockConfiguration/Rule)
             :roomkey.aws.s3.Bucket.properties/ObjectLockRule))))

(t/deftest ^:unit full-specs
  (let [parsed-spec (-> "aws/CloudFormationResourceSpecification.json"
                        io/resource
                        io/reader
                        parse/parse)]
    (t/is (= (specs/gen-specs parsed-spec specs/primitive-type->predicate)
             nil))))
