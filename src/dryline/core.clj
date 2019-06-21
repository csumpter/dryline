(ns dryline.core
  (:require [cheshire.core :as json]))

(def aws-spec (json/parse-string (slurp "resources/aws/us-east-spec.json")))
