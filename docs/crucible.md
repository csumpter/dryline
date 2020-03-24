# Crucible Integration
Dryline can easily be integrated with [Crucible](https://www.github.com/brabster/crucible). 

To run this example please include the following dependencies in your `deps.edn` file

```clojure
{roomkey.dryline {:git/url "git@github.com:roomkey/dryline.git"
                  :sha "<SHA>"}
 crucible {:mvn/version "0.43.1"}
 com.cognitect.aws/api {:mvn/version "0.8.408"}
 com.cognitect.aws/endpoints {:mvn/version "1.1.11.684"}
 com.cognitect.aws/cloudformation {:mvn/version "773.2.575.0"}}
```

The following example code shows you how to:

1. Create a compatible primitive type mapping
2. Generate Dryline based Crucible resources
3. Build and validate a sample CloudFormation template
4. Deploy the template using [congnitect-labs/aws-api](https://github.com/cognitect-labs/aws-api)

```clojure 
(ns crucible-integration
  (:require [roomkey.dryline.specs :as specs]
            [roomkey.dryline.keywords :as kws]
            [cognitect.aws.client.api :as aws]
            [clojure.java.io :as io]
            [crucible.core
             :as crucible
             :refer [xref]]
            [crucible.parameters :as param]
            [crucible.values]
            [crucible.resources]))

;;; Create a compatible primitive type mapping
;;;
;;; Because Crucible relies on internal functions like `xref` to do template
;;; level validation, you must extend Dryline with a new primitive type mapping
;;; so that resources with referenced values are valid.

(def primitive-type->crucible-spec
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
;;; In order for Crucible to do spec validation, all resources are defined using
;;; `defresource`. To use Dryline specs you ;;; must run `defresource` for all
;;; of the specs you want to use.

(defn create-crucible-resource-from-spec
  "Generates and evaluates Crucible defresource code for resource-type-name.
  The resulting function will be in the same namespace as the Dryline spec
  for the resource."
  [resource-type-identifier]
  (let [spec-kw (kws/resource-type-keyword resource-type-identifier)]
    (binding [*ns* (create-ns (symbol (namespace spec-kw)))]
      (eval `(crucible.resources/defresource ~(symbol spec-kw)
               ~resource-type-identifier
               ~spec-kw)))))

(let [parsed-spec (-> "path/to/Specification.json"
                      io/reader
                      parse/parse)]
  ;; Generate the dryline specs using a primitive type mapping that supports spec-or-ref
  (specs/add-specs parsed-spec primitive-type->crucible-spec)

  ;; Override crucible.encoding.keys/-> for all Dryline keywords
  (doseq [spec-kw spec-kws]
    (eval `(defmethod crucible.encoding.keys/->key ~spec-kw [_#]
             ~(name spec-kw))))

  ;; Run defresource for all of the Resource Types in the specification file
  (doseq [[resource-type-identifier _] (:ResourceTypes parsed-spec)]
    (create-crucible-resource-from-spec resource-type-identifier)))

;;; Build, validate and deploy a sample CloudFormation template
;;;
;;; This template aims to mirror the example found here
;;; https://s3-us-east-2.amazonaws.com/cloudformation-templates-us-east-2/SQS_With_CloudWatch_Alarms.template

(def sample-template
  ;; The call to crucible.core/template will validate each resource
  (crucible/template
   {:alarm-email (crucible/parameter ::param/description "EMail address to notify if there are any operational issues"
                                     ::param/type "String"
                                     ::param/allowed-pattern "([a-zA-Z0-9_\\-\\.]+)@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.)|(([a-zA-Z0-9\\-]+\\.)+))([a-zA-Z]{2,4}|[0-9]{1,3})(\\]?)"
                                     ::param/constraint-description "must be a valid email address.")
    :my-queue (roomkey.aws.sqs/Queue {})
    :alarm-topic (roomkey.aws.sns/Topic
                  {:Subscription [{:Endpoint (xref :alarm-email)
                                   :Protocol "email"}]})
    :queue-depth-alarm (roomkey.aws.cloudwatch/Alarm
                        {:AlarmDescription "Alarm if queue depth grows beyond 10 messages"
                         :Namespace "AWS/SQS"
                         :MetricName "ApproximateNumberOfMessagesVisible"
                         :Dimensions [{:Name "QueueName"
                                       :Value (xref :my-queue :queue-name)}]
                         :Statistic "Sum"
                         :Period 300
                         :EvaluationPeriods 1
                         :Threshold 10.0
                         :ComparisonOperator "GreaterThanThreshold"
                         :AlarmActions [(xref :alarm-topic)]
                         :InsufficientDataActions [(xref :alarm-topic)]})
    :queue-url (crucible/output (xref :my-queue) "URL of newly created SQS Queue")
    :queue-arn (crucible/output (xref :my-queue :arn) "ARN of newly created SQS Queue")
    :queue-name (crucible/output (xref :my-queue :queue-name) "Name of newly created SQS Queue")}
   "AWS CloudFormation Sample Template SQS_With_CloudWatch_Alarms: Sample template showing how to create an SQS queue with AWS CloudWatch alarms on queue depth. **WARNING** This template creates an Amazon SQS Queue and one or more Amazon CloudWatch alarms. You will be billed for the AWS resources used if you create a stack from this template."))


;;; Deploy the stack using cognitect-labs/aws-api
;;;
;;; Replace you@company.com with your own email!

(def cf-client (aws/client {:api :cloudformation}))

(aws/invoke cf-client {:op :CreateStack
                            :request {:StackName "ExampleSQSStack"
                                      :TemplateBody (crucible/encode sample-template)
                                      :Parameters [{:ParameterKey "AlarmEmail"
                                                    :ParameterValue "you@company.com"}]}})

```
