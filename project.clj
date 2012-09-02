(defproject ns-tracker "0.1.2"
  :description "Keep track of which namespaces have been modified"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [org.clojure/tools.namespace "0.1.3"]
                 [org.clojure/java.classpath "0.2.0"]]
  :profiles
  {:dev {:dependencies [[commons-io "1.4"]]}})
