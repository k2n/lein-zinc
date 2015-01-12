(ns leiningen.zinc
  (:require [leiningen.classpath :as classpath]
            [leiningen.core.main :as main]
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]
            [leiningen.help :as help]
            [clojure.string :as string]
            [clojure.java.io :as io])
  (:import  (com.typesafe.zinc Inputs Nailgun Setup Util)
            (java.io File)
            (sbt Level)
            (sbt.inc IncOptions)
            (scala Option)))

(defn- map-kv "Applies the function to both key and value of the given map." 
  [f m]
  (into {} (map (fn [entry]
                  {(f (key entry))  (f (val entry))})
                m)))

(defn- dependency "Gets a dependency from project.clj." 
  [project dependency-id]
  (let [{:keys [dependencies]} project
        dep (first (filter (fn [dependency]
                  (let [[id ver] dependency]
                    (= id dependency-id))) dependencies))]
    (if dep dep 
        (main/warn (str dependency-id " is not defined in 
                    :dependencies of project.clj." )))))

(defn- plugin "Gets a plugin from project.clj." [project plugin-id]
  (let [{:keys [plugins]} project
        dep (first (filter (fn [plugin]
                  (let [[id ver] plugin]
                    (= id plugin-id))) plugins))]
    (main/debug "plugins: " plugins)
    (if dep dep 
        (main/warn (str plugin-id " is not defined in 
                    :plugins of project.clj." )))))

(defn- dependency-version "Returns version from lein style dependency." 
  [dependency]
  (let [[_ version] dependency]
    version))

(defn- lein-dep-to-maven-dep "Converts maven style dependency notation 
                             to lein style." 
  [id]
  (let [[group_id artifact_id] (string/split id #"/")]
    [(string/replace group_id "." "/") artifact_id]))

(defn- append-classifier "Appends classifier to maven style dependency." 
  [^String classifier]
  (if classifier
    (str "-" classifier)
    ""))

(defn- user-home "JVM user.home" [] (System/getProperty "user.home"))

(defn- user-dir "JVM user.dir" [] (System/getProperty "user.dir"))

(defn- maven-local-repo-path "Construct file path of maven local repo from 
                             maven style dependency notation." 
  [id version & options]
  (let [[group-id artifact-id] (lein-dep-to-maven-dep id)
        [classifier _] options]
  (str (user-home) "/.m2/repository/" 
    group-id "/" artifact-id "/" version "/" artifact-id "-" version 
    (append-classifier classifier) ".jar")))

(defn- to-file "Converts path to java.io.File. Prepend 'user.dir' 
               if the path is relative." 
  [^String path]
  (if path
     (if (.startsWith path "/")
      (io/file path)
      (io/file (str (user-dir) "/" path)))))

(defn- to-files "Converts the paths delimited by the delimiter to seq of 
                java.io.File." 
  [^String paths, delimiter]
  (let [trimmedPaths (string/replace paths " " "")]
  (map #(to-file %) (string/split trimmedPaths (re-pattern delimiter)))))

(defn- to-seq "Converts string with the delimiter to seq." 
[^String input delimiter]
  (flatten (string/split (string/replace input " " "") 
    (re-pattern delimiter))))

(defn zinc-setup "Instantiates zinc setup object." [project] 
  (let [{:keys [sbt-version scala-version]} project]
  (Setup/create 
    (to-file (maven-local-repo-path 
              "org.scala-lang/scala-compiler" scala-version))
    (to-file (maven-local-repo-path 
              "org.scala-lang/scala-library" scala-version))
    [(to-file 
      (maven-local-repo-path 
              "org.scala-lang/scala-reflect" scala-version)) ]
    (to-file (maven-local-repo-path 
              "com.typesafe.sbt/sbt-interface" sbt-version))
    (to-file (maven-local-repo-path 
              "com.typesafe.sbt/compiler-interface" sbt-version "sources"))
    (to-file (System/getProperty "java.home"))
    true)))

(defn- is-dir? "Checks if the given file is a directory or not." [^File file]
  (.isDirectory file))

(defn- ends-with-suffix? "Checks if the file name ends with the given suffix." 
  [path suffixes]
  (not= nil (some #(.endsWith path %) suffixes)))

(defn- source-file-seq "doc-string" [^String source]
  (->> (file-seq (to-file source))
       (filter #(not (is-dir? %)))
       (filter #(ends-with-suffix? (.getCanonicalPath %) 
                                   [".scala" ".java"]))))

(defn- sources-file-seq "Converts the elements in a seq from string path 
                        to java.io.File." 
  [sources]
  (flatten (map #(source-file-seq %) sources)))

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

(defn zincInputs "Instantiates zinc inputs." [project inc-options test?]
  (let [classpath (classpath/get-classpath-string project)
        {:keys [sources test-sources classes test-classes scalac-options 
                javac-options analysis-cache analysis-map compile-order 
                mirror-analysis-cache]
         :or {sources               ["src/scala" "src/java"]
              test-sources          ["test/scala" "test/java"]
              classes               "target/classes"
              test-classes          "target/test-classes"
              scalac-options        []
              javac-options         []
              analysis-cache        "target/analysis/compile"
              analysis-map          {"/tmp" "/tmp"}
              compile-order         "Mixed"
              mirror-analysis-cache false}} 
                                          (:inputs (:zinc-options project))]
  (main/debug "classpath: " (map #(to-file %) (to-seq classpath ":")))
  (main/debug "analysis-cache: " analysis-cache)
  (main/debug "analysis-map: " analysis-map)
  (Inputs/create (map #(to-file %) (to-seq classpath ":")) 
                 (if test? (sources-file-seq test-sources) 
                           (sources-file-seq sources)) 
                 (if test? (to-file test-classes) (to-file classes)) 
                 (to-seq scalac-options ",")
                 (to-seq javac-options ",") (to-file analysis-cache) 
                 (map-kv to-file analysis-map) 
                 compile-order inc-options 
                 mirror-analysis-cache)))

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
        api-diff-context-size (option (to-file api-dump-directory)) 
        transactional? (option (to-file backup)) recompile-on-macro-def 
        name-hashing?))) 

(defn zinc-compile "Compiles Java and Scala source." [project]
  (let [logger (zinc-logger project)]
    (.compile (zincCompiler (zinc-setup project) logger) 
              (zincInputs project 
              (inc-options project) false) logger)))

(defn zinc-test-compile "Compiles Java and Scala test source." [project]
  (let [logger (zinc-logger project)]
    (.compile (zincCompiler (zinc-setup project) logger) 
              (zincInputs project 
              (inc-options project) true) logger)))

(defn zinc-profile [project] 
  (main/info "zinc-profile project: " project)
  (let [{:keys [sbt-version scala-version] 
         :or {sbt-version "0.13.6"
              scala-version (dependency-version 
                          (dependency project 'org.scala-lang/scala-library))
              }} project
        lein-zinc-version (dependency-version 
                            (plugin project 'lein-zinc/lein-zinc))]
  (main/info "scala version: " scala-version)
  (main/info "sbt version: " sbt-version)
  {:dependencies [['lein-zinc lein-zinc-version]
                  ['org.scala-lang/scala-compiler scala-version]
                  ['org.scala-lang/scala-library scala-version]
                  ['org.scala-lang/scala-reflect scala-version]
                  ['com.typesafe.sbt/sbt-interface sbt-version]
                  ['com.typesafe.sbt/compiler-interface sbt-version 
                                                :classifier "sources"]]
   :sbt-version sbt-version
   :scala-version scala-version}))

(defn zinc 
  "Compiles Scala and Java code with Typesafe zinc incremental compiler."
  {:subtasks [#'zinc-compile #'zinc-test-compile]}
    ([project] 
      (let [profile (or (:zinc (:profiles project)) 
                                          (zinc-profile project))
            project (project/merge-profiles project [profile])]
        (zinc-compile project)(zinc-test-compile project)))
    ([project subtask & options]
      (let [profile (or (:zinc (:profiles project)) (zinc-profile project))
            project (project/merge-profiles project [profile])]
        (case subtask
          "zinc-compile" (zinc-compile project)
          "zinc-test-compile" (zinc-test-compile project)
          (help/help project "zinc")))))

;; vim: set ts=2 sw=2 cc=80 et: 
