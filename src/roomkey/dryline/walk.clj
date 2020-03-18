(ns roomkey.dryline.walk
  "Utilities relating to walking the graph of property types"
  (:require [clojure.string :as string]
            [clojure.zip :as z]))

(defn- primitive-property-specification?
  "Returns true if a property specification is primitive; false otherwise. A
  property is primitive if it defines either a PrimitiveType or a
  PrimitiveItemType"
  [{:keys [PrimitiveType PrimitiveItemType] :as _property-specification}]
  (boolean (or PrimitiveType PrimitiveItemType)))

(defn- referenced-property-type-identifier
  "Returns the property type identifier for a that is referenced by a property
  specification"
  [property-type-identifier {:keys [ItemType Type] :as _property-specification}]
  (let [referenced-type (case Type
                          ("List" "Map") ItemType
                          Type)]
    (case referenced-type
      "Tag" referenced-type
      (str (first (string/split property-type-identifier #"\."))
           \.
           referenced-type))))

(defn- referenced-property-type-identifiers
  "Returns a collection of the property type identifiers that are referenced by
  a property type"
  [[property-type-identifier property-type-specification :as _property-type]]
  (sequence (comp (remove primitive-property-specification?)
                  (map (partial referenced-property-type-identifier
                                property-type-identifier)))
            (vals (:Properties property-type-specification))))

(defn root-property-types
  "Given a collection of all property types, returns the property types which
  are not referenced by any other property type"
  [property-types]
  (remove (comp (set (mapcat referenced-property-type-identifiers
                             property-types))
                first)
          property-types))

(defn property-type-zipper
  "Returns a zipper that can walk a graph of dependent property-types"
  [property-types root-property-type]
  (z/zipper

   ;; A property type is a leaf node if it has any non-primitive properties
   (fn [[_ {:keys [Properties]}]]
     (boolean (seq (remove (comp primitive-property-specification? second)
                           Properties))))

   ;; The children of a property-type node are the property types referenced
   ;; in its properties
   (fn [property-type]
     (select-keys property-types
                  (referenced-property-type-identifiers property-type)))

   (fn [n _children] n)
   root-property-type))

(defn property-type-walk
  "Walks the dependency graph of property types ensuring that specs are
  generated in the correct order."
  [spec-generator-fn zipper]
  (loop [loc zipper
         came-up? false]
    (cond
      (nil? loc) nil

      came-up?
      (do
        (spec-generator-fn (z/node loc))
        (if (z/right loc)
          (recur (z/right loc) false)
          (recur (z/up loc) true)))

      :else
      (if (and (z/branch? loc)
               (not= (z/node (z/down loc))
                     (z/node loc)))
        (recur (z/down loc) false)
        (do
          (spec-generator-fn (z/node loc))
          (if (z/right loc)
            (recur (z/right loc) false)
            (recur (z/up loc) true)))))))
