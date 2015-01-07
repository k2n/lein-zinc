(ns leiningen.zinc_test
  (:require [leiningen.zinc :refer :all]
            [expectations :refer :all])
  (:import (java.io File)))


(expect "-sources"
  (#'leiningen.zinc/append-classifier "sources"))

(expect ""
  (#'leiningen.zinc/append-classifier nil))

(expect (str (System/getProperty "user.home") "/.m2/repository" 
         "/group-id/artifact-id/1.0.0/artifact-id-1.0.0.jar")
  (#'leiningen.zinc/maven-local-repo-path "group-id/artifact-id" "1.0.0"))

(expect (str (System/getProperty "user.home") "/.m2/repository" 
         "/group-id/artifact-id/1.0.0/artifact-id-1.0.0-sources.jar")
  (#'leiningen.zinc/maven-local-repo-path 
      "group-id/artifact-id" "1.0.0" "sources"))

(expect ['org.scala-lang/scala-library "2.10.4"]
  (#'leiningen.zinc/scala-library 
    {:dependencies [['org.scala-lang/scala-library "2.10.4"]
                    ['foo/bar "1.0.0"]]}))

(expect nil
  (#'leiningen.zinc/scala-library 
    {:dependencies [['org.scala-lang/scala-TYPO "2.10.4"]
                    ['foo/bar "1.0.0"]]}))

(expect "2.10.4" 
  (#'leiningen.zinc/dependency-version (#'leiningen.zinc/scala-library 
    {:dependencies [['org.scala-lang/scala-library "2.10.4"]
                    ['foo/bar "1.0.0"]]})))

(expect ["org/apache/commons" "commons-io"]
  (#'leiningen.zinc/lein-dep-to-maven-dep "org.apache.commons/commons-io"))

(expect (new File "/tmp/foo")
  (#'leiningen.zinc/to-file "/tmp/foo"))
  
(expect (new File (str (System/getProperty "user.dir") "/foo"))
  (#'leiningen.zinc/to-file "foo"))
  
(expect (new File "/tmp/bar")
  (nth (#'leiningen.zinc/to-files "/tmp/foo, /tmp/bar" ",") 1))

(expect '[""]
  (#'leiningen.zinc/to-seq "" ","))

(expect '["foo"]
  (#'leiningen.zinc/to-seq "foo" ","))

(expect '["foo", "bar"]
  (#'leiningen.zinc/to-seq "foo, bar" ","))

(expect true
  (#'leiningen.zinc/ends-with-suffix? "foo.txt" ["txt" "log"]))

(expect false
  (#'leiningen.zinc/ends-with-suffix? "foo.txt" ["xml" "log"]))

(expect false
  (#'leiningen.zinc/ends-with-suffix? "foo.txt.un~" ["txt" "log"]))

(expect  (new java.io.File (str (System/getProperty "user.dir") "/test-project/src/scala/Test.scala"))
   (first (#'leiningen.zinc/source-file-seq "test-project/src/scala")))


;; vim: set ts=2 sw=2 cc=80 et: 
