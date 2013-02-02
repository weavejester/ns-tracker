(ns ns-tracker.core
  "Keeps track of which namespaces have changed and need to be reloaded."
  (:use [ns-tracker.dependency :only (graph seq-union depend dependents remove-key)]
        [ns-tracker.nsdeps :only (deps-from-ns-decl)]
        [clojure.java.io :only (file)]
        [clojure.tools.namespace
          [find  :only (find-clojure-sources-in-dir)]
          [parse :only (comment? ns-decl?)]])
  (:require [clojure.java.io :as io])
  (:import (java.io PushbackReader)))

(defn- file? [f]
  (instance? java.io.File f))

(defn- find-sources
  [dirs]
  {:pre [(every? file? dirs)]}
  (mapcat find-clojure-sources-in-dir dirs))

(defn- current-timestamp-map
  "Get the current modified timestamp map for all sources"
  [dirs]
  (into {} (map (fn [f] {f (.lastModified f)}) (find-sources dirs))))

(defn- modified?
  "Compare a file to a timestamp map to see if it's been modified since."
  [then now file]
  (> (get now file 0) (get then file 0)))

(defn- newer-sources [then now]
  (filter (partial modified? then now) (keys now)))

(defn ns-in?
  "Returns true if form is a (in-ns ...) declaration."
  [form]
  (and (list? form) (= 'in-ns (first form))))

(defn read-ns-decl
  "Attempts to read a (ns ...) or (in-ns ...) declaration from a
  java.io.PushbackReader. Returns [form nil] for ns declarations and
  [nil form] for in-ns declarations, and [nil nil] if read fails or
  if a ns or in-ns declaration cannot be found.  The ns/in-ns
  declaration must be the first Clojure form in the file, except
  for (comment ...) forms. Based on the function with the same
  name in core.tools.namespace.parse"
  [rdr]
  (try
   (loop [] (let [form (doto (read rdr) str)]
              (cond
               (ns-decl? form) [form nil]
               (ns-in? form) [nil form]
               (comment? form) (recur)
               :else [nil nil])))
       (catch Exception e [nil nil])))

(defn read-file-ns-decl
  "Attempts to read a (ns ...) or (in-ns ...) declaration from file.
  Returns [form nil] for ns declarations and [nil form] for in-ns
  declarations, and [nil nil] if read fails or if a ns or in-ns
  declaration cannot be found. Based on the function with the same
  name in core.tools.namespace.file"
  [file]
  (with-open [rdr (PushbackReader. (io/reader file))]
    (read-ns-decl rdr)))

(defn- newer-namespace-decls [then now]
  (loop [new-decls []
         new-names  #{}
         files    (newer-sources then now)]
    (let [file (first files)]
      (if file
        (let [[ns-decl ns-in] (read-file-ns-decl file)]
          (if ns-decl
            (recur (conj new-decls ns-decl) (conj new-names (second ns-decl)) (rest files))
            (if ns-in
              (recur new-decls (conj new-names (second (second ns-in))) (rest files))
              (recur new-decls new-names (rest files)))))
        [new-decls new-names]))))

(defn- add-to-dep-graph [dep-graph namespace-decls]
  (reduce (fn [g decl]
	    (let [nn (second decl)
		  deps (deps-from-ns-decl decl)]
	      (apply depend g nn deps)))
	  dep-graph namespace-decls))

(defn- remove-from-dep-graph [dep-graph new-decls]
  (apply remove-key dep-graph (map second new-decls)))

(defn- update-dependency-graph [dep-graph new-decls]
  (-> dep-graph
      (remove-from-dep-graph new-decls)
      (add-to-dep-graph new-decls)))

(defn- affected-namespaces [changed-namespaces old-dependency-graph]
  (apply seq-union changed-namespaces
                   (map #(dependents old-dependency-graph %)
                        changed-namespaces)))

(defn- make-file [f]
  {:pre [(or (string? f) (file? f))]}
  (if (file? f) f (file f)))

(defn- normalize-dirs [dirs]
  {:pre [(or (string? dirs) (sequential? dirs))]}
  (cond
   (string? dirs)     [(file dirs)]
   (sequential? dirs) (map make-file dirs)))

(defn ns-tracker
  "Returns a no-arg function which, when called, returns a set of
  namespaces that need to be reloaded, based on file modification
  timestamps and the graph of namespace dependencies."
  ([dirs]
     (ns-tracker dirs (current-timestamp-map (normalize-dirs dirs))))
  ([dirs initial-timestamp-map]
     {:pre [(map? initial-timestamp-map)]}
     (let [dirs (normalize-dirs dirs)
           timestamp-map (atom initial-timestamp-map)
           [init-decls init-names] (newer-namespace-decls {} @timestamp-map)
           dependency-graph (atom (update-dependency-graph (graph) init-decls))]
       (fn []
         (let [then @timestamp-map
               now (current-timestamp-map (normalize-dirs dirs))
               [new-decls new-names] (newer-namespace-decls then now)]
           (when (seq new-names)
             (let [ affected-names
                   (affected-namespaces new-names @dependency-graph)]
               (println "Reload Namespaces: " (pr-str affected-names))
               (reset! timestamp-map now)
               (swap! dependency-graph update-dependency-graph new-decls)
               affected-names)))))))
