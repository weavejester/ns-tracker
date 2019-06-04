(defproject ns-tracker "0.3.1"
  :description "Keep track of which namespaces have been modified"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/java.classpath "0.2.3"]]
  :aliases
  {"test-all" ["with-profile" "default:+1.6:+1.7:+1.8:+1.9:+1.10" "test"]}
  :profiles
  {:dev  {:dependencies [[commons-io "2.5"]]}
   :1.6  {:dependencies [[org.clojure/clojure "1.6.0"]]}
   :1.7  {:dependencies [[org.clojure/clojure "1.7.0"]]}
   :1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
   :1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
   :1.10 {:dependencies [[org.clojure/clojure "1.10.0"]]}})
