(ns zinc.lein_test
  (:require [zinc.lein :refer :all]
            [expectations :refer :all])
  (:import (java.io File)))

(expect "-sources"
  (#'zinc.lein/append-classifier "sources"))

(expect ""
  (#'zinc.lein/append-classifier nil))

(expect (str (System/getProperty "user.home") "/.m2/repository" 
         "/group-id/artifact-id/1.0.0/artifact-id-1.0.0.jar")
  (maven-local-repo-path "group-id/artifact-id" "1.0.0"))

(expect (str (System/getProperty "user.home") "/.m2/repository" 
         "/group-id/artifact-id/1.0.0/artifact-id-1.0.0-sources.jar")
  (maven-local-repo-path 
      "group-id/artifact-id" "1.0.0" "sources"))

(expect ['org.scala-lang/scala-library "2.10.4"]
  (dependency
    {:dependencies [['org.scala-lang/scala-library "2.10.4"]
                    ['foo/bar "1.0.0"]]} 'org.scala-lang/scala-library))

(expect nil
  (dependency
    {:dependencies [['org.scala-lang/scala-TYPO "2.10.4"]
                    ['foo/bar "1.0.0"]]} 'org.scala-lang/scala-library))

(expect "2.10.4" 
  (dependency-version (dependency
    {:dependencies [['org.scala-lang/scala-library "2.10.4"]
                    ['foo/bar "1.0.0"]]} 'org.scala-lang/scala-library)))

(expect ["org/apache/commons" "commons-io"]
  (#'zinc.lein/lein-dep-to-maven-dep "org.apache.commons/commons-io"))

;; vim: set ts=2 sw=2 cc=80 et: 
