(ns zinc.core
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as string]
            [clojure.tools.namespace.file :as file])
  (:import (java.io File)))

(defn map-kv 
  "Applies the function to both key and value of the given map." 
  [f m]
  (into {} (map (fn [entry]
                  {(f (key entry))  (f (val entry))})
                m)))

(defn user-home "JVM user.home" [] (System/getProperty "user.home"))

(defn user-dir "JVM user.dir" [] (System/getProperty "user.dir"))

(defn to-file 
  "Converts path to java.io.File. Prepend 'user.dir' if the path is relative." 
  [^String path]
  (if path
     (if (.startsWith path "/")
      (io/file path)
      (io/file (str (user-dir) "/" path)))))

(defn is-dir? "Checks if the given file is a directory or not." [^File file]
  (.isDirectory file))

(defn to-seq "Converts string with the delimiter to seq." 
  [^String input delimiter]
  (flatten (string/split (string/replace input " " "") 
    (re-pattern delimiter))))

(defn ends-with-suffix? "Checks if the file name ends with the given suffix." 
  [suffixes path]
  (not= nil (some #(.endsWith path %) suffixes)))

(defn scala-or-java-file? 
  "Checks if the given file has '.scala' or '.java' suffix." 
  [^File file]
  (->> file
       (.getCanonicalPath)
       (ends-with-suffix? [".scala" ".java"])))

(defn source-file-seq 
  "Search the files underneath the given path and return Scala or Java source 
  files." 
  [^String source]
  (->> (file-seq (to-file source))
       (filter #(not (is-dir? ^File %)))
       (filter #(scala-or-java-file? ^File %))))

(defn sources-file-seq 
  "Converts the elements in a seq from string path to java.io.File and filters 
  non Scala/Java source files." 
  [sources]
  (flatten (map #(source-file-seq %) sources)))

(defn- find-files [dirs]
  (->> dirs
       (map #(to-file %))
       (filter #(.exists ^File %))
       (mapcat file-seq)
       (filter scala-or-java-file?)
       (map #(.getCanonicalFile ^File %))))

(defn- modified-files [tracker files]
  (filter #(< (::time tracker 0) (.lastModified ^File %)) files))
 
(defn- deleted-files [tracker files]
  (set/difference (::files tracker #{}) (set files)))
 
(defn- update-files [tracker deleted modified]
  (let [now (System/currentTimeMillis)]
    (-> tracker
        (update-in [::files] #(if % (apply disj % deleted) #{}))
        (file/remove-files deleted)
        (update-in [::files] into modified)
        (file/add-files modified)
        (assoc ::time now))))

(defn scan "doc-string" [tracker dirs]
  (let [files (find-files dirs)
        deleted (seq (deleted-files tracker files))
        modified (seq (modified-files tracker files))]
      (if (or deleted modified)
        (update-files tracker deleted modified)
        tracker)))

;; vim: set ts=3 sw=2 cc=80 et: 
