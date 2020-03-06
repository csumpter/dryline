(ns roomkey.dryline.documentation
  (:require [hickory.core :as hick]
            [hickory.select :as hick.sel]
            [clojure.java.io]
            [clojure.string]
            [markdown.core]
            [roomkey.dryline.util :refer [dryline-keyword]]
            [clojure.walk :refer [postwalk]]
            [clojure.pprint]))

(defn- load-as-hickory [md-file-path]
  (-> (slurp md-file-path)
      markdown.core/md-to-html-string
      hick/parse
      hick/as-hickory))

(defn- get-tag [hick-data tag]
  (hick.sel/select
   (hick.sel/child
    (hick.sel/tag tag))
   hick-data))

(defn- get-property [property hick-data]
  (remove nil? (map (fn [{:keys [content] :as original}]
                      (let [{:keys [content]} (first content)]
                        (when (= (first content) property)
                          original)))
                    hick-data)))

(defn- get-in-hickory [hick-data attr]
  (try
    (let [value (-> (drop-while (fn [{:keys [content]}]
                                      (not (= attr (first content)))) (:content (first hick-data)))
                        (nth 2)
                        :content
                        first)]
      value)
    (catch Exception e nil)))

(defmacro get-value [hick-data value & options]
  `(try
     (some-> (get-in-hickory ~hick-data ~value)
             ~@options)
     (catch Exception e# nil)))

(defn- get-title [hick-data]
  (try
    (let [h1-data (get-tag hick-data :h1)
          raw-title (-> (first h1-data)
                        :content
                        first)]
      (if (> (count raw-title) 1)
        (dryline-keyword (clojure.string/join "." (clojure.string/split raw-title #"\s")))
        (dryline-keyword raw-title)))
    (catch Exception e nil)))

(defn- get-properties [hick-data]
  (remove nil? (map (fn [{:keys [content]}]
                      (when (vector? content)
                        (-> content
                            first
                            :content
                            first)))
                    hick-data)))

(defn- remove-nils
  [m]
  (let [f (fn [[k v]] (when v [k v]))]
    (postwalk (fn [x] (if (map? x) (into {} (map f x)) x)) m)))

(defn- ?assoc
  "Same as assoc, but skip the assoc if v is nil"
  [m & kvs]
  (->> kvs
       (partition 2)
       (filter second)
       (map vec)
       (into m)))

(defn- scrape-file [path]
  (let [file (load-as-hickory path)
        p-elements (get-tag file :p)
        properties (get-properties p-elements)
        title (get-title file)]
    {(keyword title)
     (apply merge
            (map (fn [property-name]
                   (let [prop-hick (get-property property-name p-elements)
                         regex (get-value prop-hick "Pattern" re-pattern)
                         enumerations (when-let [rtn (get-value prop-hick "Allowed Values")]
                                        (-> (clojure.string/split rtn #"\s\|\s")
                                            set))
                         minimum (get-value prop-hick "Minimum" Integer/parseInt)
                         maximum (get-value prop-hick "Maximum" Integer/parseInt)]
                     (when (or regex enumerations minimum maximum)
                       {(keyword property-name)
                        (?assoc {}
                                :regex regex
                                :enumerations enumerations
                                :minimum minimum
                                :maximum maximum)})))
                 properties))}))

(defn scrape-property-specs [path-to-repo]
  (let [property-files (remove nil?
                               (map
                                (fn [path] (when (re-matches #"^aws-properties.*" path) path))
                                (->> "doc_source/"
                                     (str path-to-repo)
                                     clojure.java.io/file
                                     .list
                                     seq)))
        shaped-prop-files (map #(str path-to-repo "doc_source/" %) property-files)
        scraped-data (->> shaped-prop-files
                          (map scrape-file)
                          (apply merge)
                          remove-nils)]
    scraped-data))

(comment

(scrape-property-specs ".../aws-cloudformation-user-guide/")

(clojure.pprint/pprint
 (scrape-property-specs ".../aws-cloudformation-user-guide/")
 (clojure.java.io/writer "resources/scraped-spec-data.edn"))

)

(comment

(def aaa (scrape-file ".../aws-cloudformation-user-guide/doc_source/aws-properties-cw-alarm.md"))

(def bbb (scrape-file ".../aws-cloudformation-user-guide/doc_source/aws-properties-lambda-function-code.md"))

(merge (scrape-file ".../aws-cloudformation-user-guide/doc_source/aws-properties-cw-alarm.md")
         (scrape-file ".../aws-cloudformation-user-guide/doc_source/aws-properties-lambda-function-code.md"))

(scrape-file ".../aws-cloudformation-user-guide/doc_source/aws-properties-cw-alarm.md")
)
