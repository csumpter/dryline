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
    (t/is (= (sut "AWS::ManagedBlockchain::Member.VotingPolicy"
                  "ApprovalThresholdPolicy")
             :roomkey.aws.managedblockchain.Member/ApprovalThresholdPolicy))
    (t/is (= (sut "AWS::PinpointEmail::ConfigurationSet"
                  "Tag")
             :roomkey.aws/Tag))))
