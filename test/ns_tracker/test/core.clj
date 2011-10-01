(ns ns-tracker.test.core
  (:import org.apache.commons.io.FileUtils)
  (:use ns-tracker.core
        clojure.test
        [clojure.java.io :only (file)]))

(deftest test-ns-tracker
  (.mkdirs (file "tmp/example"))
  (try
    (let [modified-namespaces (ns-tracker [(file "tmp")])]
      (testing "modified files are reloaded"
        (Thread/sleep 1000)
        (spit (file "tmp/example/core.clj") '(ns example.core))
        (is (= (modified-namespaces) #{'example.core}))
        (is (empty? (modified-namespaces))))
      
      (testing "dependant files are reloaded"
        (Thread/sleep 1000)
        (spit (file "tmp/example/util.clj") '(ns example.util))
        (spit (file "tmp/example/core.clj")
              '(ns example.core (:use example.util)))
        (modified-namespaces)
        (Thread/sleep 1000)
        (spit (file "tmp/example/util.clj") '(ns example.util))
        (is (= (modified-namespaces) #{'example.core 'example.util}))))
    (finally
     (FileUtils/deleteDirectory (file "tmp")))))
