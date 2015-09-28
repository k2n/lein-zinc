(ns leiningen.circle
  (:require [leiningen
             [core.eval :as evl]
             [release   :as release]
             [deploy    :as deploy]]))

(defn env 
  "Obtain environment variable" 
  [s]
  (System/getenv s))

(defn circle 
  "doc-string" 
  [project & args]
  )

;; vim: set ts=2 sw=2 cc=80 et:
