(ns roomkey.dryline.parse
  (:require [cheshire.core :as json]))

(defn parse
  "Parses an AWS CloudFormation specification passed in as a reader."
  [rdr]
  (json/parse-stream rdr (fn [k]
                           (cond
                             ;; Specs with only one resource defined
                             ;; have a singular key so we change it
                             ;; to plural here. Thx AWS.
                             (= k "ResourceType") :ResourceTypes
                             (= k "Tag") "Tag"
                             (re-matches #".+::.+::.+" k) k
                             :else (keyword k)))))
