(defproject lein-zinc "0.1.0-SNAPSHOT"
  :description "Typesafe zinc scala incremental compiler plugin"
  :url "https://github.com/k2n/lein-zinc"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :eval-in-leiningen true
  :dependencies [[com.typesafe.zinc/zinc "0.3.5.3"]
                 [leiningen "2.5.0"]
                ;; TODO remove the following deps once the plugin 
                ;; downloads the jars
                ;; used by sbt.
                 [org.scala-lang/scala-compiler "2.11.4"]
                 [org.scala-lang/scala-reflect "2.11.4"]
                 [org.scala-lang/scala-library "2.11.4"]
                 [com.typesafe.sbt/sbt-interface "0.13.6"]
                 [com.typesafe.sbt/compiler-interface "0.13.6" 
                  :classifier "sources"]]
  :plugins [[lein-expectations "0.0.8"]
            [lein-ancient "0.5.5"]]
  :aliases  {"test"  ["expectations"]}
  :profiles {:dev {:global-vars {*warn-on-reflection* true}
                   :dependencies [[expectations "2.0.13"]]}})

;; vim: set ts=2 sw=2 cc=80 et: 
