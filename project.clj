(defproject ns-tracker "0.4.0"
  :description "Keep track of which namespaces have been modified"
  :url "https://github.com/weavejester/ns-tracker"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.namespace "1.1.0"]
                 [org.clojure/java.classpath "1.0.0"]]
  :aliases
  {"test-all" ["with-profile" "default:+1.6:+1.7:+1.8:+1.9:+1.10" "test"]}
  :profiles
  {:dev  {:dependencies [[commons-io "2.6"]]}
   :1.6  {:dependencies [[org.clojure/clojure "1.6.0"]]}
   :1.7  {:dependencies [[org.clojure/clojure "1.7.0"]]}
   :1.8  {:dependencies [[org.clojure/clojure "1.8.0"]]}
   :1.9  {:dependencies [[org.clojure/clojure "1.9.0"]]}
   :1.10 {:dependencies [[org.clojure/clojure "1.10.0"]]}})
