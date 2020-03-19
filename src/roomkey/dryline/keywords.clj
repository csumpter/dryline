(ns roomkey.dryline.keywords
  (:require [clojure.string :as string]))

(defn- split-type-identifier
  "Splits an AWS type identifier of the form into a tuple of
  (<serviceprovider> <servicename> <ResourceTypeName> ?<PropertyTypeName>).
  The tuple will contain a fourth element of PropertyTypeName iff DataTypeName
  contains a `.` signifying that it is a property type. Note that ServiceProvider
  and ServiceName will be returned in lower case in the tuple."
  [type-identifier]
  (-> (string/split type-identifier #"::")
      (update 0 string/lower-case)
      (update 1 string/lower-case)
      (update 2 string/split #"\.")
      flatten))

(defn- make-namespace
  "Builds a dot concatenated namespace string from `args` with the first value
  being \"roomkey\""
  [& args]
  (string/join \. (into ["roomkey"] args)))

(defn resource-type-keyword
  "Converts an AWS resource type identifier into a keyword of form
  :roomkey.<serviceprovider>.<servicename>/<ResourceTypeName> "
  [resource-type-identifier]
  (let [[provider service resource-type-name]
        (split-type-identifier resource-type-identifier)]
    (keyword (make-namespace provider service)
             resource-type-name)))

(defn resource-type-property-keyword
  "Returns a keyword for a property of a resource type of form
  :roomkey.<serviceprovider>.<servicename>.<ResourceTypeName>/<PropertyIdentifier>"
  [resource-type-identifier property-identifier]
  (let [[provider service resource-type]
        (split-type-identifier resource-type-identifier)]
    (keyword (make-namespace provider service resource-type)
             (name property-identifier))))

(defn property-type-keyword
  "Converts an AWS property type identifier to a keyword of form
  :roomkey.<serviceprovider>.<servicename>.<ResourceTypeName>.properties/<PropertyTypeName>"
  [property-type-identifier]
  (case property-type-identifier
    "Tag" :roomkey.aws/Tag
    (let [[provider service resource-type-name property-type-name]
          (split-type-identifier property-type-identifier)]
      (keyword (make-namespace provider service resource-type-name "properties")
               property-type-name))))

(defn property-type-property-keyword
  "Returns a keyword for a property of a property type of form
  :roomkey.<serviceprovider>.<servicename>.<ResourceTypeName>.properties.<PropertyTypeName>/<PropertyIdentifier>"
  [property-type-identifier property-identifier]
  (case property-type-identifier
    "Tag" (keyword "roomkey.aws.Tag" (name property-identifier))
    (let [[provider service resource-type-name property-type-name]
          (split-type-identifier property-type-identifier)]
      (keyword (make-namespace provider service resource-type-name "properties"
                               property-type-name)
               (name property-identifier)))))

(defn referenced-property-type-keyword
  "Returns the spec keyword for a referenced property type within the property
  specification of a resource type or property type.
  E.g. AWS::S3::Bucket.ObjectLockConfiguration, ObjectLockRule ->
  :roomkey.aws.s3.Bucket.properties/ObjectLockRule"
  [type-identifier property-identifier]
  (case property-identifier
    "Tag" :roomkey.aws/Tag
    (let [[provider service resource-type-name]
          (split-type-identifier type-identifier)]
      (keyword (make-namespace provider service resource-type-name "properties")
               (name property-identifier)))))
