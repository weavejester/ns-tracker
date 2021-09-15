(ns ns-tracker.nsdeps-test
  (:require [clojure.java.io :as io]
            [clojure.string :refer [trim-newline]]
            [clojure.test :refer :all]
            [ns-tracker.nsdeps :refer :all])
  (:import (org.apache.commons.io FileUtils)
           (java.io StringWriter)))

(defmacro with-err-str [& body]
  `(let [s# (new StringWriter)]
     (binding [*err* s#]
       ~@body
       (str s#))))

(deftest test-deps-from-ns-decl
  (let [tmp-dir (io/file "tmp")]
    (try
      (.mkdirs (io/file "tmp/sql"))
      (spit (io/file "tmp/sql/foo.sql") "")

      (testing "resource-deps metadata, reader macro"
        (is (= #{(io/file "tmp/sql/foo.sql")}
               (deps-from-ns-decl
                 '(ns ^{:ns-tracker/resource-deps ["sql/foo.sql"]} example.db)
                 [tmp-dir]))))

      (testing "resource-deps metadata, attr-map"
        (is (= #{(io/file "tmp/sql/foo.sql")}
               (deps-from-ns-decl
                 '(ns example.db
                    {:ns-tracker/resource-deps ["sql/foo.sql"]})
                 [tmp-dir]))))

      (testing "resource-deps metadata, docstring & attr-map"
        (is (= #{(io/file "tmp/sql/foo.sql")}
               (deps-from-ns-decl
                 '(ns example.db
                    "docstring"
                    {:ns-tracker/resource-deps ["sql/foo.sql"]})
                 [tmp-dir]))))

      (testing "metadata from reader macro and attr-map is merged"
        (is (= #{(io/file "tmp/sql/foo.sql")}
               (deps-from-ns-decl
                 '(ns ^{:ns-tracker/resource-deps ["sql/foo.sql"]} example.db
                    {})
                 [tmp-dir])))
        (is (= #{(io/file "tmp/sql/foo.sql")}
               (deps-from-ns-decl
                 '(ns ^{} example.db
                    {:ns-tracker/resource-deps ["sql/foo.sql"]})
                 [tmp-dir])))
        (is (= #{(io/file "tmp/sql/foo.sql")}
               (deps-from-ns-decl
                 '(ns ^{:ns-tracker/resource-deps ["this-will-be-overridden"]} example.db
                    {:ns-tracker/resource-deps ["sql/foo.sql"]})
                 [tmp-dir]))))

      (testing "prints a warning to stderr if the resource is not found"
        (let [result (atom nil)
              stderr (with-err-str
                       (reset! result
                               (deps-from-ns-decl
                                 '(ns ^{:ns-tracker/resource-deps ["sql/bar.sql"]} example.db)
                                 [tmp-dir])))]
          (is (= #{} @result))
          (is (= "ns-tracker: Unable to track dependency from namespace example.db to resource \"sql/bar.sql\". The resource was not found in directories [\"tmp\"]."
                 (trim-newline stderr)))))

      (testing "ignores `:as-alias` require"
        (is (= #{'example.foo}
               (deps-from-ns-decl
                '(ns example.db (:require [example.foo :as foo]
                                          [example.bar :as-alias bar]))
                [tmp-dir]))))

      (finally
        (FileUtils/deleteDirectory tmp-dir)))))
