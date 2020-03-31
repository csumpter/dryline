(ns roomkey.dryline-test
  (:require [clojure.java.io :as io]
            [clojure.test :as t]
            [roomkey.dryline :as dryline]
            [roomkey.dryline.simple-intrinsic-function :as sif]))

(t/deftest ^:unit parse-specification-and-add-specs
  (t/is (nil? (dryline/parse-specification-and-add-specs
               (-> "aws/S3BucketSpecification.json"
                   io/resource
                   io/reader)
               sif/primitive-type->spec
               :validate true))))

(t/deftest ^:unit validate-and-encode
  (let [bucket {:Type "AWS::S3::Bucket"
                :Properties {:BucketName "my-bucket"}}
        template {:AWSTemplateFormatVersion "2010-09-09"
                  :Resources {"MyBucket" bucket}
                  :Outputs {"BucketARN" {:Description "The ARN of MyBucket"
                                         :Value {"Fn::GetAtt" ["MyBucket" "Arn"]}}
                            "BucketName" {:Description "The name of MyBucket"
                                          :Value {"Ref" "MyBucket"}}}}]
    (t/is (string? (dryline/encode template :validate true)))))
