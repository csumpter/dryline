(ns roomkey.dryline.documentation
  (:require [http.async.client :as http]
            [hickory.core :as hick]
            [hickory.select :as hick.sel]
            [clojure.zip :as z]))

(defn get-documentation-as-hickory
  [url]
  (with-open [client (http/create-client :max-redirects 5)]
    (-> (http/GET client url)
        http/await
        http/string
        hick/parse
        hick/as-hickory)))

(def properties-selector
  "A hickory selector for finding the properties section"
  (hick.sel/and (hick.sel/tag :h2)
                (hick.sel/find-in-text #"^Properties")))

(def variable-list-selector
  "A hickory selector for finding a variable list"
  (hick.sel/and (hick.sel/tag :div)
                (hick.sel/class "variablelist")))

(def properties-list-selector
  "A hickory selector for finding the list of properties"
  (hick.sel/follow properties-selector variable-list-selector))

(defn property-dt-selector
  "Returns a hickory selector for the dt element (property-name) in a properties
  list for the given property-name"
  [property-name]
  (hick.sel/descendant
   properties-list-selector
   (hick.sel/and (hick.sel/tag :dt)
                 (hick.sel/has-descendant
                  (hick.sel/find-in-text (re-pattern (name property-name)))))))

(defn property-dd-selector
  "Returns a hickory selector for the dd element (property description) in a
  properties list for a given property-name"
  [property-name]
  (hick.sel/follow-adjacent (property-dt-selector property-name)
                            (hick.sel/tag :dd)))

(comment
  (def url "https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-properties-sqs-queues.html")

  (def docs (get-documentation-as-hickory url))

  (hick.sel/select (hick.sel/descendant properties-list-selector
                                        variable-list-dt-selector) docs)

  ;; get the dt element that has the variable name
  (hick.sel/select (property-dt-selector :ContentBasedDeduplication) docs)
  ;; get the dd element for the variable name
  (hick.sel/select (property-dd-selector :ContentBasedDeduplication) docs))
