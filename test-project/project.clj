(defproject test-project "0.1.0-SNAPSHOT"
  :description "test project using lein-zinc"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-zinc "0.1.0-SNAPSHOT"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.scala-lang/scala-library "2.11.4"]]
  :prep-tasks ["zinc" "compile"]
  :profiles {:zinc-custom-options 
             {:zinc-options 
              {:logging 
               {:level     "debug"
                :colorize? false}
               :inputs 
                {:sources         ["src/scala" "src/java"] 
                 :test-sources    ["test/scala" "test/java"]
                 :classes         "target/classes"
                 :test-classes    "target/test-classes"
                 :scalac-options  []
                 :javac-options   []
                 :analysis-cache  "target/analysis/compile"
                 :analysis-map    {"path_of_a_source_dir_of_other_project" 
                                   "path_of_analysis-cache_of_other_project"}
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
                 }}}

             :custom-scala-version
              {:scala-version "2.10.4"
               :sbt-version "0.13.5"
               :zinc-options 
                {:logging
                 {:level "info"}}}})

;; vim: set ts=2 sw=2 cc=80 et: 
