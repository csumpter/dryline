# Crucible Integration
Dryline can easily be integrated with [Crucible](https://www.github.com/brabster/crucible). 

## Translate the spec-or-ref concept
Because Crucible relies on internal functions like `xref` to do template level validation, you must extend Dryline with a new primitive type mapping so that resources with referenced values are valid.

```clojure 
(ns crucible-integration
  (:require [roomkey.dryline.parse :as parse]
            [roomkey.dryline.specs :as specs]
            [clojure.java.io :as io]
            [crucible.core :refer [template xref output encode]]
            [crucible.policies :as p]
            [crucible.resources :as r]))

;;; Translate the spec-or-ref concept
;;; 
;;; Because Crucible relies on internal functions like `xref` 
;;; to do template level validation, you must extend Dryline 
;;; with a new primitive type mapping so that resources with 
;;; referenced values are valid.
            
(def primitive-type->predicate
  {"String" '(clojure.spec.alpha/or
              :primitive string?
              :reference :crucible.values/value)
   "Long" '(clojure.spec.alpha/or
            :primitive int?
            :reference :crucible.values/value)
   "Integer" '(clojure.spec.alpha/or
               :primitive int?
               :reference :crucible.values/value)
   "Double" '(clojure.spec.alpha/or
              :primitive double?
              :reference :crucible.values/value)
   "Boolean" '(clojure.spec.alpha/or
               :primitive boolean?
               :reference :crucible.values/value)
   "Timestamp" '(clojure.spec.alpha/or
                 :primitive inst?
                 :reference :crucible.values/value)
   "Json" '(clojure.spec.alpha/or
            :primitive map?
            :reference :crucible.values/value)})
            
;;; Generate Dryline based defresources
;;;
;;; In order for Crucible to do spec validation, all resources
;;; are defined using `defresource`. To use Dryline specs you 
;;; must run `defresource` for all of the specs you want to use.

(defn create-crucible-resource-from-spec
  "Generates and evaluates Crucible defresource code for resource-type-name. 
  The resulting function will be in the same namespace as the Dryline spec 
  for the resource."
  [resource-type-name]
  (let [spec-kw (specs/dryline-keyword resource-type-name)]
    (binding [*ns* (create-ns (symbol (namespace spec-kw)))]
      (eval `(crucible.resources/defresource ~(symbol spec-kw)
               ~resource-type-name
               ~spec-kw)))))

(let [parsed-spec (-> "path/to/Specification.json"
                      io/resource
                      io/reader
                      parse/parse)]
  ;; Generate the dryline specs using a primitive type mapping that supports spec-or-ref                    
  (specs/gen-specs parsed-spec primitive-type->predicate)

  ;; Run defresource for all of the Resource Types in the specification file
  (doseq [[resource-type-name _] (:ResourceTypes parsed-spec)]
    (create-crucible-resource-from-spec resource-type-name)))

;;; Build and validate a sample CloudFormation template

(template "Example CF Stack"
          {:s3-bucket (roomkey.aws.s3/Bucket {:BucketName "MyBucket"}})
```