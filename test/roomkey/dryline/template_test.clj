(ns roomkey.dryline.template-test
  (:require [clojure.java.io :as io]
            [clojure.test :as t]
            [roomkey.dryline :as dryline]
            [roomkey.dryline.simple-intrinsic-function :as sif]
            [roomkey.dryline.template :as template]
            [clojure.spec.alpha :as s]))


(dryline/parse-specification-and-add-specs
 (-> "aws/CloudFormationResourceSpecification.json"
     io/resource
     io/reader)
 sif/primitive-type->spec
 :validate true)

(t/deftest ^:unit good-template
  (let [bucket {:Type "AWS::S3::Bucket"
                :Properties {:BucketName "my-bucket"}}
        queue {:Type "AWS::SQS::Queue"
               :Properties {:QueueName "my-queue"}}
        template {:AWSTemplateFormatVersion "2010-09-09"
                  :Resources {"MyBucket" bucket
                              "MyQueue" queue}
                  :Outputs {"BucketARN" {:Description "The ARN of MyBucket"
                                         :Value {"Fn::GetAtt" ["MyBucket" "Arn"]}}
                            "BucketName" {:Description "The name of MyBucket"
                                          :Value {"Ref" "MyBucket"}}}}]
    (t/is (s/valid? ::template/template template))))

(t/deftest ^:unit bad-template
  (let [bucket {:Type "AWS::S3::Bucket"
                :Properties {:BucketName 123}} ; this should be a string
        template {:AWSTemplateFormatVersion "2010-09-09"
                  :Resources {"MyBucket" bucket}}]
    (t/is (not (s/valid? ::template/template template)))))
