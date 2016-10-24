(ns ns-tracker.test.core
  (:import org.apache.commons.io.FileUtils)
  (:use ns-tracker.core
        clojure.test
        [clojure.java.io :only (file)]))

(deftest test-ns-tracker
  (.mkdirs (file "tmp/example"))
  (.mkdirs (file "tmp/example/internal"))
  (try
    (let [modified-namespaces (ns-tracker [(file "tmp")])]
      (testing "modified files are reloaded"
        (Thread/sleep 1000)
        (spit (file "tmp/example/core.clj") '(ns example.core))
        (is (= (modified-namespaces) '(example.core)))
        (is (empty? (modified-namespaces))))
      
      (testing "dependant files are reloaded"
        (Thread/sleep 1000)
        (spit (file "tmp/example/util.clj") '(ns example.util))
        (spit (file "tmp/example/core.clj")
              '(ns example.core (:use example.util)))
        (modified-namespaces)
        (Thread/sleep 1000)
        (spit (file "tmp/example/util.clj") '(ns example.util))
        (is (= (modified-namespaces) '(example.util example.core))))

      (testing "can handle in-ns forms"
        (Thread/sleep 1000)
        (spit (file "tmp/example/internal/util.clj") '(in-ns example.util))
        (spit (file "tmp/example/util.clj")
              (pr-str '(ns example.util) '(load "example/internal/util")))
        (spit (file "tmp/example/core.clj")
              '(ns example.core (:use example.util)))
        (modified-namespaces)
        (Thread/sleep 1000)
        (spit (file "tmp/example/internal/util.clj") '(in-ns example.util))
        (is (= (modified-namespaces) '(example.util example.core))))

      (testing "namespaces are returned in an order suited for reloading"
        (Thread/sleep 1000)
        (spit (file "tmp/example/a.clj") '(ns example.a))
        (spit (file "tmp/example/b.clj") '(ns example.b (:use example.a)))
        (spit (file "tmp/example/c.clj") '(ns example.c (:use example.b)))
        (spit (file "tmp/example/d.clj") '(ns example.d (:use example.c)))
        (modified-namespaces)
        (Thread/sleep 1000)
        (spit (file "tmp/example/a.clj") '(ns example.a))
        (is (= (modified-namespaces)
               '(example.a example.b example.c example.d))))

      (when-not (neg? (compare (clojure-version) "1.7.0"))
        (testing "modified cljc files are reloaded (Clojure 1.7+)"
          (Thread/sleep 1000)
          (spit (file "tmp/example/a_cljc.cljc") '(ns example.a-cljc))
          (is (= (modified-namespaces) '(example.a-cljc)))
          (is (empty? (modified-namespaces)))
          (Thread/sleep 1000)
          (spit (file "tmp/example/b_cljc.cljc") 
                "(ns example.b-cljc (:require #?(:clj [example.a-cljc] :cljs [example.a-cljc])))")
          (is (= (modified-namespaces) '(example.b-cljc)))
          (Thread/sleep 1000)
          (spit (file "tmp/example/a_cljc.cljc") '(ns example.a-cljc (:require clojure.set)))        
          (is (= (modified-namespaces) '(example.a-cljc example.b-cljc)))
          (is (empty? (modified-namespaces))))))

    (testing "directories can be supplied as strings"
      (let [modified-namespaces (ns-tracker ["tmp"])]
        (Thread/sleep 1000)
        (spit (file "tmp/example/core.clj") '(ns example.core))
        (is (= (modified-namespaces) '(example.core)))
        (is (empty? (modified-namespaces)))))

    (testing "can supply directory as single string"
      (let [modified-namespaces (ns-tracker "tmp")]
        (Thread/sleep 1000)
        (spit (file "tmp/example/core.clj") '(ns example.core))
        (is (= (modified-namespaces) '(example.core)))
        (is (empty? (modified-namespaces)))))

    (testing "dependencies of older files accounted for"
      (spit (file "tmp/example/util.clj") '(ns example.util))
      (spit (file "tmp/example/core.clj")
            '(ns example.core (:use example.util)))
      (Thread/sleep 1000)
      (let [modified-namespaces (ns-tracker [(file "tmp")])]
        (Thread/sleep 1000)
        (spit (file "tmp/example/util.clj") '(ns example.util))
        (is (= (modified-namespaces) '(example.util example.core)))))

    (testing "libspecs with a prefix list can be handled properly"
      (spit (file "tmp/example/x.clj") '(ns example.x
                                          (:require (example.internal [y :as y]
                                                                      [z :as z]))))
      (spit (file "tmp/example/internal/y.clj") '(ns example.internal.y))
      (spit (file "tmp/example/internal/z.clj") '(ns example.internal.z))
      (Thread/sleep 1000)
      (let [modified-namespaces (ns-tracker [(file "tmp")])]
        (Thread/sleep 1000)
        (spit (file "tmp/example/internal/y.clj") '(ns example.internal.y))
        (is (= (modified-namespaces) '(example.internal.y example.x)))))

    (testing "a prefix list represented with a vector can also be handled properly"
      (spit (file "tmp/example/x.clj") '(ns example.x
                                          (:require [example.internal [y :as y]
                                                                      [z :as z]])))
      (spit (file "tmp/example/internal/y.clj") '(ns example.internal.y))
      (spit (file "tmp/example/internal/z.clj") '(ns example.internal.z))
      (Thread/sleep 1000)
      (let [modified-namespaces (ns-tracker [(file "tmp")])]
        (Thread/sleep 1000)
        (spit (file "tmp/example/internal/y.clj") '(ns example.internal.y))
        (is (= (modified-namespaces) '(example.internal.y example.x)))))

    (testing "circular dependencies throw an AssertionError"
      (spit (file "tmp/example/ns1.clj") '(ns example.ns1 (:require [example.ns2])))
      (spit (file "tmp/example/ns2.clj") '(ns example.ns2 (:require [example.ns1])))
      (Thread/sleep 1000)
      (is (thrown? AssertionError (ns-tracker [(file "tmp")]))))

    (testing "self-dependencies throw an AssertionError"
      (spit (file "tmp/example/ns1.clj") '(ns example.ns1 (:require [example.ns1])))
      (Thread/sleep 1000)
      (is (thrown? AssertionError (ns-tracker [(file "tmp")]))))

    (finally
     (FileUtils/deleteDirectory (file "tmp")))))
