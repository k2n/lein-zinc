(ns zinc.lein
  "Handles leiningen project access and conversions with maven."
  (:require [clojure.string :as string]
            [leiningen.core.main :as main]
            [zinc.core :as core]))

(defn dependency "Gets a dependency from project.clj." 
  [project dependency-id]
  (let [{:keys [dependencies]} project
        dep (first (filter (fn [dependency]
                  (let [[id ver] dependency]
                    (= id dependency-id))) dependencies))]
    (if dep dep 
        (main/warn (str dependency-id " is not defined in 
                    :dependencies of project.clj." )))))

(defn plugin "Gets a plugin from project.clj." [project plugin-id]
  (let [{:keys [plugins]} project
        dep (first (filter (fn [plugin]
                  (let [[id ver] plugin]
                    (= id plugin-id))) plugins))]
    (main/debug "plugins: " plugins)
    (if dep dep 
        (main/warn (str plugin-id " is not defined in 
                    :plugins of project.clj." )))))

(defn dependency-version "Returns version from lein style dependency." 
  [dependency]
  (let [[_ version] dependency]
    version))

(defn- lein-dep-to-maven-dep 
 "Converts maven style dependency notation to lein style." 
  [id]
  (let [[group_id artifact_id] (string/split id #"/")]
    [(string/replace group_id "." "/") artifact_id]))

(defn- append-classifier "Appends classifier to maven style dependency." 
  [^String classifier]
  (if classifier
    (str "-" classifier)
    ""))

(defn maven-local-repo-path 
  "Construct file path of maven local repo from maven style dependency." 
  [id version & options]
  (let [[group-id artifact-id] (lein-dep-to-maven-dep id)
        [classifier _] options]
  (str (core/user-home) "/.m2/repository/" 
    group-id "/" artifact-id "/" version "/" artifact-id "-" version 
    (append-classifier classifier) ".jar")))


;; vim: set ts=2 sw=2 cc=80 et: 
