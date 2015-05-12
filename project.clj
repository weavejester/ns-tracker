(defproject ns-tracker "0.2.2"
  :description "Keep track of which namespaces have been modified"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.namespace "0.2.10"]
                 [org.clojure/java.classpath "0.2.2"]]
  :profiles
  {:dev {:dependencies [[commons-io "1.4"]]}
   :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
   :1.7 {:dependencies [[org.clojure/clojure "1.7.0-beta2"]]}})
