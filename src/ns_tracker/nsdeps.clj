(ns ns-tracker.nsdeps
  "Parsing namespace declarations for dependency information."
  (:import java.io.File)
  (:require [clojure.set :refer [union]]))

(defn- deps-from-libspec [prefix form]
  (cond (list? form) (deps-from-libspec prefix (vec form))
        (vector? form) (if (and (>= (count form) 2)
                                (not (keyword? (second form))))
                         (let [prefix (and prefix (str prefix "."))]
                           (->> (rest form)
                                (map #(deps-from-libspec (symbol (str prefix (first form))) %))
                                (apply union)))
                         (deps-from-libspec prefix (first form)))
	(symbol? form) #{(symbol (str (when prefix (str prefix ".")) form))}
	(keyword? form) #{}
	:else (throw (IllegalArgumentException.
		      (pr-str "Unparsable namespace form:" form)))))

(defn- deps-from-ns-form [form]
  (when (and (list? form)
	     (contains? #{:use :require} (first form)))
    (apply union (map #(deps-from-libspec nil %) (rest form)))))

(defn- find-resource [path dirs]
  (->> (map #(File. ^File % ^String path) dirs)
       (filter #(.isFile ^File %))
       first))

(defn- print-warnings-for-missing-resources [namespace resources dirs]
  (doseq [resource (->> resources
                        (filter #(nil? (second %)))
                        (map first))]
    (binding [*out* *err*]
      (let [dirs (vec (map #(.getPath ^File %) dirs))]
        (println (str "ns-tracker: Unable to track dependency from namespace "
                      namespace " to resource \"" resource "\". The resource "
                      "was not found in directories " dirs "."))))))

(defn- ns-attr-map [[_ _ docstring? attr-map?]]
  (cond
    (map? docstring?) docstring?
    (map? attr-map?) attr-map?))

(defn- ns-metadata [[_ name :as decl]]
  (merge (meta name)
         (ns-attr-map decl)))

(defn- deps-from-ns-metadata [[_ name :as decl] dirs]
  (let [deps (:ns-tracker/resource-deps (ns-metadata decl))
        resources (for [dep deps]
                    [dep (find-resource dep dirs)])]
    (print-warnings-for-missing-resources name resources dirs)
    (set (remove nil? (map second resources)))))

(defn deps-from-ns-decl
  "Given a (quoted) ns declaration and list of source directories, returns
  a set of symbols and files naming the dependencies of that namespace.
  Namespace dependencies are symbols and resource dependencies are java.io.File
  instances. Handles :use and :require clauses, and :ns-tracker/resource-deps
  metadata."
  [decl dirs]
  (union
    (deps-from-ns-metadata decl dirs)
    (apply union (map deps-from-ns-form decl))))
