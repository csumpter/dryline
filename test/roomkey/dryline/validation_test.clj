(ns roomkey.dryline.validation-test
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.test :as t]
            [roomkey.dryline.parse :as parse]
            [roomkey.dryline.validation]))

(def parsed-spec (parse/parse (io/reader (io/resource "aws/us-east-spec.json"))))

(t/deftest ^:unit valid-specification
  (t/is (s/valid? :roomkey.dryline.validation/Specification parsed-spec)))

