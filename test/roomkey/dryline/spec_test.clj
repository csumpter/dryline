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
