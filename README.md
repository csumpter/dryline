

#### Source materials

https://d1uauaxba7bl26.cloudfront.net/latest/gzip/CloudFormationResourceSpecification.json

https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cfn-resource-specification-format.html


```
us-east-spec.json

        +
        |
        |
        |
        v
    parse    +--------->    clojure map (cheshire)
        +
        |
        |
        v
   find
   aws S3 spec      AWS::S3::Bucket
        +
        |
        |
        |
        v
   write S3 spec   +---------->    validate template
                                   (and create CFN stack?)
```
