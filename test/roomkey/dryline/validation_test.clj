(ns roomkey.dryline.validation-test
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.test :as t]
            [roomkey.dryline.parse :as parse]
            [roomkey.dryline.validation]))

(def parsed-spec (-> "aws/CloudFormationResourceSpecification.json"
                     io/resource
                     io/reader
                     parse/parse))

(t/deftest ^:unit valid-specification
  (t/is (s/valid? :roomkey.dryline.validation/Specification parsed-spec)))

