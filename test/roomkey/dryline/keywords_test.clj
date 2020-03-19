(ns roomkey.dryline.keywords-test
  (:require [clojure.test :as t]
            [roomkey.dryline.keywords :as kws]))

(t/deftest ^:unit resource-type-keyword
  (let [sut kws/resource-type-keyword]
    (t/is (= (sut "AWS::WAFv2::WebACL")
             :roomkey.aws.wafv2/WebACL))))

(t/deftest ^:unit resource-type-property-keyword
  (let [sut kws/resource-type-property-keyword]
    (t/is (= (sut "AWS::WAFv2::WebACL" :Rules)
             :roomkey.aws.wafv2.WebACL/Rules))))

(t/deftest ^:unit property-type-keyword
  (let [sut kws/property-type-keyword]
    (t/is (= (sut "AWS::WAFv2::WebACL.Rules")
             :roomkey.aws.wafv2.WebACL.properties/Rules))))

(t/deftest ^:unit property-type-property-keyword
  (let [sut kws/property-type-property-keyword]
    (t/is (= (sut "AWS::WAFv2::WebACL.Rules" :Rules)
             :roomkey.aws.wafv2.WebACL.properties.Rules/Rules))))

(t/deftest ^:unit referenced-property-type-keyword
  (let [sut kws/referenced-property-type-keyword]
    (t/is (= (sut "AWS::WAFv2::WebACL.Rules" :Rule)
             :roomkey.aws.wafv2.WebACL.properties/Rule))))

