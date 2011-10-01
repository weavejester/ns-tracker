(ns ns-tracker.test.core
  (:import org.apache.commons.io.FileUtils)
  (:use ns-tracker.core
        clojure.test
        [clojure.java.io :only (file)]))

(deftest test-ns-tracker
  (.mkdirs (file "tmp/example"))
  (try
    (let [modified-namespaces (ns-tracker [(file "tmp")])]
      (Thread/sleep 1000)
      (spit (file "tmp/example/core.clj") '(ns example.core))
      (is (= (modified-namespaces) #{'example.core}))
      (is (empty? (modified-namespaces))))
    (finally
     (FileUtils/deleteDirectory (file "tmp")))))
