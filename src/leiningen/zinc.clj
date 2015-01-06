(ns leiningen.zinc
  (:require [leiningen.classpath :as classpath]
            [leiningen.core.main :as main]
            [leiningen.core.eval :as eval]
            [leiningen.core.project :as project]
            [clojure.string :as string])
  (:import  (com.typesafe.zinc Inputs Setup Util)
            (sbt Level)
            (sbt.inc IncOptions)))

;; TODO :scala-source-path needs to be unified with the one 
;; passed into sbt.
;; TODO :scalac-options
(comment (defn task-props "doc-string" [project]
  (merge {:srcdir (:scala-source-path project)
          :destdir (:compile-path project)}
          (:scalac-options project))))

(defn- scala-library "obtain scala-library dependency from project.clj" 
  [project]
  (let [{:keys [dependencies]} project
        dep (first (filter (fn [dependency]
                  (let [[id ver] dependency]
                    (= id 'org.scala-lang/scala-library))) dependencies))]
    (if dep dep 
        (main/warn "scala-library is not defined in 
                    :dependencies of project.clj." ))))

(defn- dependency-version "return version from lein style dependency" 
  [dependency]
  (let [[_ version] dependency]
    version))

(defn- lein-dep-to-maven-dep "convert maven style dependency notation 
                             to lein style" 
  [id]
  (let [[group_id artifact_id] (string/split id #"/")]
    [(string/replace group_id "." "/") artifact_id]))

(defn- append-classifier "append classifier to maven style dependency" 
  [^String classifier]
  (if classifier
    (str "-" classifier)
    ""))

(defn- user-home "JVM user.home" [] (System/getProperty "user.home"))

(defn- user-dir "JVM user.dir" [] (System/getProperty "user.dir"))

(defn- maven-local-repo-path "construct file path of maven local repo from 
                             maven style dependency notation" 
  [id version & options]
  (let [[group-id artifact-id] (lein-dep-to-maven-dep id)
        [classifier _] options]
  (str (user-home) "/.m2/repository/" 
    group-id "/" artifact-id "/" version "/" artifact-id "-" version 
    (append-classifier classifier) ".jar")))

(defn- to-file "convert path to java.io.File. Prepend 'user.dir' 
               if the path is relative" 
  [^String path]
  (if path
    (if (.startsWith path "/")
      (new java.io.File path)
      (new java.io.File (str (user-dir) "/" path)))))

(defn- to-files "convert the paths delimited by the delimiter to seq of 
                java.io.File" 
  [^String paths, delimiter]
  (let [trimmedPaths (string/replace paths " " "")]
  (map #(to-file %) (string/split trimmedPaths (re-pattern delimiter)))))

(defn- to-seq "convert string with the delimiter to seq" 
[^String input delimiter]
  (flatten (string/split (string/replace input " " "") 
    (re-pattern delimiter))))

;; TODO: download the jars and source jars for the version inferred from 
;; scala-library in the target project dependencies up front so that the
;; code can assure the jars are in maven local repo.
(defn zinc-setup "instantiate zinc setup object" [] 
  (Setup/create 
    (to-file (maven-local-repo-path "org.scala-lang/scala-compiler" "2.11.4"))
    (to-file (maven-local-repo-path "org.scala-lang/scala-library" "2.11.4"))
    [(to-file 
      (maven-local-repo-path "org.scala-lang/scala-reflect" "2.11.4")) ]
    (to-file (maven-local-repo-path "com.typesafe.sbt/sbt-interface" "0.13.6"))
    (to-file (maven-local-repo-path 
              "com.typesafe.sbt/compiler-interface" "0.13.6" "sources"))
    (to-file (System/getProperty "java.home"))
    true))

;; TODO create multiple levels of config under zinc-options
(defn- zinc-options-in  [project]
  (:zinc-options project))

(defn zinc-logger "instantiate zinc logger" [project]
  (let [{:keys [level colorize?]
         :or {level "info"
              colorize? false}} (:logging (:zinc-options project))]
    (Util/logger false (Level/withName level) colorize?)))

(defn zincCompiler "instantiate zinc compiler" 
  [zinc-setup zinc-logger]
  (com.typesafe.zinc.Compiler/getOrCreate zinc-setup zinc-logger))

;; TODO rename parameters to clojure style
;; TODO create a function to return the seq of files under the specified 
;; directories for scalaSources
;; TODO convert options type to take seq in project.clj
;; TODO implement the logic to inherit the analysis cache in the
;; parent projects
(defn zincInputs "instantiate zinc inputs" [project inc-options]
  (let [classpath (classpath/get-classpath-string project)
        {:keys [scalaSources classesDirectory scalacOptions javacOptions 
                analysisCache analysisMap compileOrder mirrorAnalysisCache]
         :or {scalaSources "src/scala/Test.scala"
              classesDirectory "target/classes"
              scalacOptions []
              javacOptions []
              analysisCache "target/zinc-analysis-cache"
              analysisMap {(to-file "/tmp") (to-file "/tmp")}
              compileOrder "Mixed"
              mirrorAnalysisCache false}} (zinc-options-in project)]
  (main/debug "classpath: " (map #(to-file %) (to-seq classpath ":")))
  (Inputs/create (map #(to-file %) (to-seq classpath ":")) 
                 (map #(to-file %) (to-seq scalaSources ",")) 
                 (to-file classesDirectory) (to-seq scalacOptions ",")
                 (to-seq javacOptions ",") (to-file analysisCache) analysisMap 
                 compileOrder inc-options mirrorAnalysisCache)))

(defn inc-options "options for sbt incremental compiler" [project]
  (let [defaultIncOptions (sbt.inc.IncOptions/Default) 
        {:keys [transitiveStep recompileAllFraction relationsDebug
                apiDebug apiDiffContextSize apiDumpDirectory transactional
                backup recompileOnMacroDef nameHashing]
         :or { transitiveStep (.transitiveStep defaultIncOptions)
               recompileAllFraction (.recompileAllFraction defaultIncOptions)
               relationsDebug (.relationsDebug defaultIncOptions)
               apiDebug (.apiDebug defaultIncOptions)
               apiDiffContextSize (.apiDiffContextSize defaultIncOptions)
               apiDumpDirectory (.apiDumpDirectory defaultIncOptions)
               transactional false
               backup nil
               recompileOnMacroDef (.recompileOnMacroDef defaultIncOptions)
               nameHashing (.nameHashing defaultIncOptions)
             }} (zinc-options-in project)]
      (new com.typesafe.zinc.IncOptions transitiveStep
        recompileAllFraction relationsDebug apiDebug
        apiDiffContextSize apiDumpDirectory transactional
        backup recompileOnMacroDef nameHashing))) 

(def zinc-profile {:dependencies [['lein-zinc "0.1.0-SNAPSHOT"]]})

(defn zinc 
  "Typesafe scala zinc incremental compiler plugin."
  [project]
  (let [profile (or (:zinc (:profiles project)) zinc-profile)
        project (project/merge-profiles project [profile])
        logger (zinc-logger project)]
    (def post-project project)
    (main/debug "user dir: " (user-dir))  
    (.compile (zincCompiler (zinc-setup) logger) 
          (zincInputs project (inc-options project)) logger)))

;; vim: set ts=2 sw=2 cc=80 et: 
