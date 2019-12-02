(ns roomkey.dryline.spec-test
  (:require  [clojure.test :as t]
             [roomkey.dryline.specs :as specs]))

(t/deftest ^:unit dryline-keyword
  (let [sut specs/dryline-keyword]
    (t/are [type-name kw] (= (sut type-name) kw)
      "AWS::ManagedBlockchain::Member.ApprovalThresholdPolicy"
      :roomkey.aws.managedblockchain.Member/ApprovalThresholdPolicy
      "AWS::SNS::Topic"
      :roomkey.aws.sns/Topic
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
