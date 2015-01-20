(ns zinc.core_test
  (:require [zinc.core :refer :all]
            [expectations :refer :all])
  (:import [java.io File]))

(expect (new File "/tmp/foo")
  (to-file "/tmp/foo"))
  
(expect (new File (str (System/getProperty "user.dir") "/foo"))
  (to-file "foo"))
  
(expect '[""]
  (to-seq "" ","))

(expect '["foo"]
  (to-seq "foo" ","))

(expect '["foo", "bar"]
  (to-seq "foo, bar" ","))

(expect true
  (ends-with-suffix? ["txt" "log"] "foo.txt"))

(expect false
  (ends-with-suffix? ["xml" "log"] "foo.txt"))

(expect false
  (ends-with-suffix? ["txt" "log"] "foo.txt.un~"))

(expect  (new File (str (System/getProperty "user.dir") 
                                  "/test-project/src/scala/Test.scala"))
   (first (source-file-seq "test-project/src/scala")))

;; vim: set ts=2 sw=2 cc=80 et: 
