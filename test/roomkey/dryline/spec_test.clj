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

(t/deftest ^:unit append-to-keyword
  (let [sut #'specs/append-to-keyword]
    (t/is (= (sut :roomkey.aws.managedblockchain.Member/ApprovalThresholdPolicy
                  :ThresholdComparator)
             :roomkey.aws.managedblockchain.Member.ApprovalThresholdPolicy/ThresholdComparator))
    (t/is (= (sut :roomkey.aws.lambda/EventSourceMapping
                  :Enabled)
             :roomkey.aws.lambda.EventSourceMapping/Enabled))))

(t/deftest ^:unit spec-reference
  (let [sut #'specs/spec-reference]
    (t/is (= (sut "AWS::S3::Bucket.ObjectLockConfiguration"
                  "Rule")
             :roomkey.aws.s3.Bucket.Rule/Rule))
    (t/is (= (sut "AWS::S3::Bucket"
                  "Tag")
             :roomkey.aws/Tag))))

(t/deftest ^:unit s3-specs
  (let [parsed-spec (-> "aws/S3BucketSpecification.json"
                        io/resource
                        io/reader
                        parse/parse)]
    (specs/gen-specs parsed-spec specs/primitive-type->predicate)
    (t/is (= (s/describe :roomkey.aws.s3/Bucket)
             '(keys
               :opt-un
               (:roomkey.aws.s3.Bucket/WebsiteConfiguration
                :roomkey.aws.s3.Bucket/BucketEncryption
                :roomkey.aws.s3.Bucket/BucketName
                :roomkey.aws.s3.Bucket/LoggingConfiguration
                :roomkey.aws.s3.Bucket/ReplicationConfiguration
                :roomkey.aws.s3.Bucket/LifecycleConfiguration
                :roomkey.aws.s3.Bucket/ObjectLockConfiguration
                :roomkey.aws.s3.Bucket/CorsConfiguration
                :roomkey.aws.s3.Bucket/InventoryConfigurations
                :roomkey.aws.s3.Bucket/PublicAccessBlockConfiguration
                :roomkey.aws.s3.Bucket/MetricsConfigurations
                :roomkey.aws.s3.Bucket/NotificationConfiguration
                :roomkey.aws.s3.Bucket/ObjectLockEnabled
                :roomkey.aws.s3.Bucket/Tags
                :roomkey.aws.s3.Bucket/VersioningConfiguration
                :roomkey.aws.s3.Bucket/AccelerateConfiguration
                :roomkey.aws.s3.Bucket/AnalyticsConfigurations
                :roomkey.aws.s3.Bucket/AccessControl))))
    (t/is (= (get (s/registry) :roomkey.aws.s3.Bucket/ObjectLockConfiguration)
             :roomkey.aws.s3.Bucket.ObjectLockConfiguration/Bucket.ObjectLockConfiguration))
    (t/is (= (s/describe :roomkey.aws.s3.Bucket.ObjectLockConfiguration/Bucket.ObjectLockConfiguration)
             '(keys
               :opt-un
               (:roomkey.aws.s3.Bucket.ObjectLockConfiguration/Rule
                :roomkey.aws.s3.Bucket.ObjectLockConfiguration/ObjectLockEnabled))))
    (t/is (= (get (s/registry) :roomkey.aws.s3.Bucket.ObjectLockConfiguration/Rule)
             :roomkey.aws.s3.Bucket.ObjectLockRule/ObjectLockRule))))


(t/deftest ^:unit full-specs
  (let [parsed-spec (-> "aws/us-east-spec.json"
                        io/resource
                        io/reader
                        parse/parse)]
    (t/is (= (specs/gen-specs parsed-spec specs/primitive-type->predicate)
             nil))))
