(ns roomkey.dryline.documentation ; rename
  (:require [hickory.core :as hick]
            [hickory.select :as hick.sel]
            [clojure.java.io]
            [clojure.string]
            [markdown.core]
            [roomkey.dryline.util :refer [dryline-keyword]]
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
  (->> hick-data
       (map (fn [{:keys [content] :as original}]
              (let [{:keys [content]} (first content)]
                (when (= (first content) property)
                  original))))
       (remove nil?)))

(defn- get-in-hickory [hick-data attr]
  (-> (drop-while (fn [{:keys [content]}]
                    (not (= attr (first content)))) (:content (first hick-data)))
      (nth 2 nil)
      :content
      first))

(defmacro get-value [hick-data value & options]
  `(try
     (some-> (get-in-hickory ~hick-data ~value)
             ~@options)
     (catch Exception e# nil)))

(defn- get-title [hick-data]
  (let [h1-data (get-tag hick-data :h1)
        raw-title (-> (first h1-data)
                      :content
                      first)]
    (case raw-title
      "Resource Tag" :roomkey.aws/Tag
      (dryline-keyword (clojure.string/join "." (clojure.string/split raw-title #"\s"))))))

(defn- get-properties [hick-data]
  (->> hick-data
       (map (fn [{:keys [content]}]
              (some-> content
                      first
                      :content
                      first)))
       (remove nil?)))

(defn- property-values [p-elements property-name]
  (let [prop-hick (get-property property-name p-elements)
                                 regex (get-value prop-hick "Pattern" re-pattern)
                                 enumerations (when-let [rtn (get-value prop-hick "Allowed Values")]
                                                (-> (clojure.string/split rtn #"\s\|\s")
                                                    set))
                                 minimum (get-value prop-hick "Minimum" Integer/parseInt)
                                 maximum (get-value prop-hick "Maximum" Integer/parseInt)
                                 property-values (cond-> {}
                                                  regex (assoc :regex regex)
                                                  enumerations (assoc :enumerations enumerations)
                                                  minimum (assoc :minimum minimum)
                                                  maximum (assoc :maximum maximum))]
                             [(keyword property-name) property-values])) ;factor out

(defn- scrape-file [path]
  (let [file (load-as-hickory path)
        p-elements (get-tag file :p)
        properties (get-properties p-elements)
        title (get-title file)]
    [(keyword title)
     (into {} (comp (map (partial property-values p-elements))
                    (filter (comp seq second)))
           properties)]))

(defn scrape-property-specs [path-to-repo]
  (let [property-files (->> (->> "doc_source/"
                                 (str path-to-repo)
                                 clojure.java.io/file ;get absolute path
                                 .list
                                 seq)
                            (filter (fn [path] (when (re-matches #"^aws-properties.*" path) path)))
                            (remove #{"aws-properties-name.md"}))
        shaped-prop-files (map #(str path-to-repo "doc_source/" %) property-files)]
    (into {} (comp (map (partial scrape-file))
                   (filter (comp seq second)))
              shaped-prop-files)))

(comment

(scrape-property-specs ".../aws-cloudformation-user-guide/")

(clojure.pprint/pprint
 (scrape-property-specs ".../aws-cloudformation-user-guide/")
 (clojure.java.io/writer "resources/scraped-spec-data.edn"))

(def a (load-as-hickory "/Users/csumpter/code/aws-cloudformation-user-guide/doc_source/aws-properties-cw-alarm.md"))

(def b (get-properties a))


)

(comment

(def aaa (scrape-file ".../aws-cloudformation-user-guide/doc_source/aws-properties-cw-alarm.md"))

(def bbb (scrape-file ".../aws-cloudformation-user-guide/doc_source/aws-properties-lambda-function-code.md"))

(merge (scrape-file ".../aws-cloudformation-user-guide/doc_source/aws-properties-cw-alarm.md")
         (scrape-file ".../aws-cloudformation-user-guide/doc_source/aws-properties-lambda-function-code.md"))

(scrape-file ".../aws-cloudformation-user-guide/doc_source/aws-properties-cw-alarm.md")
)
