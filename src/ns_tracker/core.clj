(ns ns-tracker.core
  "Keeps track of which namespaces have changed and need to be reloaded."
  (:use [ns-tracker.dependency :only (graph seq-union depend dependents remove-key)]
	[ns-tracker.nsdeps :only (deps-from-ns-decl)]
        [clojure.java.io :only (file)]
	[clojure.tools.namespace :only (find-clojure-sources-in-dir
                                        read-file-ns-decl)]))

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
  [timestamp-map file]
  (> (.lastModified file) (get timestamp-map file 0)))

(defn- newer-sources [timestamp-map files]
  (filter (partial modified? timestamp-map) files))

(defn- newer-namespace-decls [timestamp-map files]
  (remove nil? (map read-file-ns-decl (newer-sources timestamp-map files))))

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
           init-decls (newer-namespace-decls {} (keys @timestamp-map))
           dependency-graph (atom (update-dependency-graph (graph) init-decls))]
       (fn []
         (let [then @timestamp-map
               now (current-timestamp-map (normalize-dirs dirs))
               new-decls (newer-namespace-decls then (keys now))]
           (when (seq new-decls)
             (let [new-names (map second new-decls)
                   affected-names
                   (affected-namespaces new-names @dependency-graph)]
               (reset! timestamp-map now)
               (swap! dependency-graph update-dependency-graph new-decls)
               affected-names)))))))