(defproject test-project "1.1.1-SNAPSHOT"
  :description "test project using lein-zinc"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-zinc "1.0.0-SNAPSHOT"]]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.scala-lang/scala-library "2.11.4"]]
  :prep-tasks ["zinc" "compile"]
  :source-paths ["src/clojure" "src/java" "src/scala"]
  :test-paths ["test/clojure" "test/java" "test/scala"]
  :profiles {:zinc-custom-options 
              {:zinc-options 
                {:logging 
                  {:level                 "debug"
                   :colorize?             false}
                 :inputs 
                   {:classes              "target/classes"
                    :test-classes         "target/test-classes"
                    :scalac-options       ["-unchecked"]
                    :javac-options        ["-deprecation" "-g"]
                    :compile-order        "Mixed"
                    :analysis-cache       "target/analysis/compile"
                    :test-analysis-cache  "target/analysis/test-compile"
                    :analysis-map         {"src_dir_of_other_project" 
                                           "analysis-cache_of_other_project"}
                   }
                 :incremental
                   {:transitive-step         3
                    :recompile-all-fraction  0.5
                    :relations-debug?        false
                    :api-debug?              false
                    :api-diff-context-size   5
                    :api-dump-directory      "target/api"
                    :transactional?          true
                    :backup                  "target/backup"
                   }
                 :continuous-compile 
                   {:interval-in-ms          2000}}}

             :custom-scala-version
               {:scala-version               "2.10.4"
                :sbt-version                 "0.13.5"
                :fork-java?                  true
                :zinc-options 
                  {:logging
                    {:level                  "info"}}}})

;; vim: set ts=2 sw=2 cc=80 et: 
