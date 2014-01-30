(defproject ns-tracker "0.2.2"
  :description "Keep track of which namespaces have been modified"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.namespace "0.2.4"]
                 [org.clojure/java.classpath "0.2.2"]]
  :profiles
  {:dev {:dependencies [[commons-io "1.4"]]}
   :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
   :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
   :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}})
