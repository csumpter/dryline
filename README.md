# Dryline
A [dry line](https://en.wikipedia.org/wiki/Dry_line) is a line across a continent that separates moist air from dry air. Storms (cloud formations) form along a dry line.

![](https://s-media-cache-ak0.pinimg.com/originals/6e/b2/c0/6eb2c06bf5a5cf0d4412de6a3fe0d72a.jpg)

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
