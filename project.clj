(defproject lein-zinc "0.1.6-SNAPSHOT"
  :description "Typesafe zinc scala incremental compiler plugin"
  :url "https://github.com/k2n/lein-zinc"
  :scm {:name "git"
        :url "https://github.com/k2n/lein-zinc"}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :pom-addition [:developers [:developer
                                [:name "Kenji Nakamura"]
                                [:email "kenjin@clazzsoft.com"]
                                [:timezone "-8"]]]
  :eval-in-leiningen true
  :sbt-version "0.13.9"
  :dependencies [[com.typesafe.zinc/zinc "0.3.9"]
                 [leiningen "2.5.3"]
                 [org.clojure/tools.namespace  "0.2.10"]]
  :plugins [[lein-expectations "0.0.8"]
            [lein-ancient "0.6.3"]
            [lein-cloverage  "1.0.2"]]
  :aliases  {"test"  ["expectations"]}
  :repositories [["k2n-clojars" {:url "https://clojars.org/repo"
                             :username [:env/clojars_username :gpg]
                             :password [:env/clojars_password :gpg]}]]
  :deploy-repositories  [["releases" :k2n-clojars]
                         ["snapshots" :k2n-clojars]]
  :signing {:gpg-key "75D0F6B0"}
  :profiles {:dev {:global-vars {*warn-on-reflection* true}
                   :dependencies [[expectations "2.1.3"]]}})

;; vim: set ts=2 sw=2 cc=80 et: 
