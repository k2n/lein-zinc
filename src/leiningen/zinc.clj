(ns leiningen.zinc
  (:require [leiningen.classpath :as classpath]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project]
            [leiningen.deps :as deps]
            [leiningen.help :as help]
            [clojure.set :as cs]
            [clojure.tools.namespace.track :as track]
            [zinc.core :as core]
            [zinc.lein :as lein])
  (:import  (com.typesafe.zinc Inputs Nailgun Setup Util)
            (sbt Level)
            (sbt.inc IncOptions)
            (scala Option)))

(def default-sources ["src/scala" "src/java"])
(def default-test-sources ["test/scala" "test/java"])

(defn zinc-setup "Instantiates zinc setup object." [project] 
  (let [{:keys [sbt-version scala-version fork-java?]} project]
  (Setup/create 
    (core/to-file (lein/maven-local-repo-path 
              "org.scala-lang/scala-compiler" scala-version))
    (core/to-file (lein/maven-local-repo-path 
              "org.scala-lang/scala-library" scala-version))
    [(core/to-file 
      (lein/maven-local-repo-path 
              "org.scala-lang/scala-reflect" scala-version)) ]
    (core/to-file (lein/maven-local-repo-path 
              "com.typesafe.sbt/sbt-interface" sbt-version))
    (core/to-file (lein/maven-local-repo-path 
              "com.typesafe.sbt/compiler-interface" sbt-version "sources"))
    (core/to-file (core/java-home))
    fork-java?)))

(defn- option "Returns scala.Some(arg) if arg is not nil else scala.None." 
  [arg]
  (if (nil? arg) (scala.Option/apply nil) (new scala.Some arg)))

(defn zinc-logger "Instantiate zinc logger." [project]
  (let [{:keys [level colorize?]
         :or {level "info"
              colorize? false}} (:logging (:zinc-options project))]
    (Util/logger false (Level/withName level) colorize?)))

(defn zincCompiler "Instantiates zinc compiler." 
  [zinc-setup zinc-logger]
  (com.typesafe.zinc.Compiler/getOrCreate zinc-setup zinc-logger))

(defn- combine-sources 
  "Merges leiningen standard source-paths, java-source-paths, and 
  zinc inputs sources. Zinc input sources is deprecated. Leiningen standard 
  paths should be used to specify the source paths as it is recognized by 
  IDEs such as IntelliJ." 
  [project]
  (into #{} (cs/union (:source-paths project)
                      (:java-source-paths project)
                      [(str (:root project) "/src/java") 
                       (str (:root project) "/src/scala")])))

(def tmpdir (System/getProperty "java.io.tmpdir"))

(defn zincInputs "Instantiates zinc inputs." [project inc-options test?]
  (let [classpath (classpath/get-classpath-string project)
        {:keys [classes test-classes scalac-options 
                javac-options analysis-cache test-analysis-cache analysis-map 
                compile-order mirror-analysis-cache]
         :or {classes               "target/classes"
              test-classes          "target/test-classes"
              scalac-options        []
              javac-options         []
              analysis-cache        "target/analysis/compile"
              test-analysis-cache   "target/analysis/test-compile"
              analysis-map          {tmpdir tmpdir}
              compile-order         "Mixed"
              mirror-analysis-cache false}} 
                                          (:inputs (:zinc-options project))
         sources (combine-sources project)
         test-sources (:test-paths project)]
    (main/debug "classpath: "(map #(core/to-file %) 
                              (core/to-seq classpath ":")))
    (main/debug "sources: " sources)
    (main/debug "test-sources: " test-sources)
    (main/debug "analysis-cache: " analysis-cache)
    (main/debug "analysis-map: " analysis-map)
    (try (Inputs/create (map #(core/to-file %) (core/to-seq classpath ":")) 
                   (if test? (core/sources-file-seq test-sources) 
                             (core/sources-file-seq sources)) 
                   (if test? (core/to-file test-classes) (core/to-file classes)) 
                   scalac-options
                   javac-options
                   (if test? (core/to-file test-analysis-cache) 
                             (core/to-file analysis-cache)) 
                   (core/map-kv core/to-file analysis-map) 
                   compile-order inc-options 
                   mirror-analysis-cache)
          (catch Exception e 
            (main/abort "Invalid parameter. " (.getMessage e))))))

(defn inc-options "Generates options for sbt incremental compiler." [project]
  (let [defaultIncOptions (sbt.inc.IncOptions/Default) 
        {:keys [transitive-step recompile-all-fraction relations-debug?
                api-debug? api-diff-context-size api-dump-directory 
                transactional? backup recompile-on-macro-def name-hashing?]
         :or { transitive-step (.transitiveStep defaultIncOptions)
               recompile-all-fraction (.recompileAllFraction defaultIncOptions)
               relations-debug? (.relationsDebug defaultIncOptions)
               api-debug? (.apiDebug defaultIncOptions)
               api-diff-context-size (.apiDiffContextSize defaultIncOptions)
               api-dump-directory nil
               transactional? false
               backup "target/backup"
               recompile-on-macro-def (.recompileOnMacroDef defaultIncOptions)
               name-hashing? (.nameHashing defaultIncOptions)
             }} 
                (:incremental (:zinc-options project))]
      (new com.typesafe.zinc.IncOptions transitive-step
        recompile-all-fraction relations-debug? api-debug?
        api-diff-context-size (option (core/to-file api-dump-directory)) 
        transactional? (option (core/to-file backup)) recompile-on-macro-def 
        name-hashing?))) 

(defn- do-compile  [project test?]
  (let [logger (zinc-logger project)]
    (try (.compile (^com.typesafe.zinc.Compiler 
                zincCompiler (zinc-setup project) logger) 
              (zincInputs project 
              (inc-options project) test?) logger)
      (catch Exception e (main/info (.getMessage e))))))

(defn zinc-compile "Compiles Java and Scala source." [project]
  (do-compile project false))

(defn zinc-test-compile "Compiles Java and Scala test source." [project]
  (do-compile project true))

(defn- continuous-compile [project sources f]
  (let [{:keys [interval-in-ms] 
         :or {interval-in-ms 2000}} (:continuous-compile 
                                      (:zinc-options project))]
    (loop [tracker (track/tracker)]
      (let [new-tracker (core/scan tracker sources)]
        (main/debug "new-tracker: " new-tracker)
        (try
          (when (not= new-tracker tracker)
            (f project)
            (main/info "compile completed."))
          
          (Thread/sleep interval-in-ms)
          (catch Exception ex (.getMessage ex)))
        (recur new-tracker)))))

(defn cc 
  "Compiles Java and Scala main sources continuously.
   This doesn't compile test source code so you may want run 
   'lein zinc test-cc' task in a separate terminal." 
  [project]
  (let [combined-sources (combine-sources project)]
    (main/info "sources:" combined-sources)
    (continuous-compile project combined-sources zinc-compile)))

(defn test-cc 
  "Compiles Java and Scala test sources continuously.
  This doesn't compile main source code so you may want run 
  'lein zinc cc' task in a separate terminal." 
  [project]
  (let [test-sources (:test-paths project)]
    (main/info "test-sources:" test-sources)
    (continuous-compile project test-sources zinc-test-compile)))

(defn zinc-profile [project] 
  "Generates lein project profile that contains the configurations necessary 
  to run lein zinc plugin."
  (let [{:keys [sbt-version scala-version fork-java?] 
         :or {sbt-version "0.13.9"
              scala-version (lein/dependency-version 
                              (lein/dependency project 
                                           'org.scala-lang/scala-library))
              fork-java? false
              }} project 
        lein-zinc-version (lein/dependency-version 
                            (lein/plugin project 'lein-zinc/lein-zinc))]
  (main/info "scala version: " scala-version)
  (main/info "sbt   version: " sbt-version)
  (main/info "fork java?     " fork-java?)
  {:dependencies [['lein-zinc lein-zinc-version]
                  ['org.scala-lang/scala-compiler scala-version]
                  ['org.scala-lang/scala-library scala-version]
                  ['org.scala-lang/scala-reflect scala-version]
                  ['com.typesafe.sbt/sbt-interface sbt-version]
                  ['com.typesafe.sbt/compiler-interface sbt-version 
                                                :classifier "sources"]]
   :sbt-version sbt-version
   :scala-version scala-version
   :fork-java? fork-java?}))

(defn zinc 
  "Compiles Scala and Java code with Typesafe zinc incremental compiler."
  {:subtasks [#'zinc-compile #'zinc-test-compile #'cc #'test-cc]}
    ([project] 
      (let [profile (or (:zinc (:profiles project)) (zinc-profile project))
            project (project/merge-profiles project [profile])]
        (deps/deps project)(zinc-compile project)(zinc-test-compile project)))
    ([project subtask & options]
      (let [profile (or (:zinc (:profiles project)) (zinc-profile project))
            project (project/merge-profiles project [profile])]
        (deps/deps project)
        (case subtask
          "zinc-compile" (zinc-compile project)
          "zinc-test-compile" (zinc-test-compile project)
          "cc" (cc project)
          "test-cc" (test-cc project)
          (help/help project "zinc")))))

;; vim: set ts=2 sw=2 cc=80 et: 
