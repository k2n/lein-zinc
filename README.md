# lein-zinc

A Leiningen plugin to compile scala and java source code with [Typesafe zinc](https://github.com/typesafehub/zinc), which is a stand-alone version of scala incremental compiler forked from sbt. 

[![Circle CI](https://circleci.com/gh/k2n/lein-zinc.svg?style=svg)](https://circleci.com/gh/k2n/lein-zinc)
[![Dependencies Status](http://jarkeeper.com/k2n/lein-zinc/status.svg)](http://jarkeeper.com/k2n/lein-zinc)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/k2n/lein-zinc?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)


[![Clojars Project](http://clojars.org/lein-zinc/latest-version.svg)](http://clojars.org/lein-zinc)

## Usage

* Put scala source code under `src/scala`, and test code under `test/scala`. The directories can be overridden. See 'Avaiable Options' below. 
* Put the latest version shown above into the `:plugins` vector of your `project.clj`.
* Add `[org.scala-lang/scala-library "your_scala_version"]` to `:dependencies` of your `project.clj`.
* To automically run zinc compiler in regular lifecycle of leiningen, add `["zinc" "compile"]` to `:prep-tasks`. 
* Alternatively, run the task directly from the command line.  

```
    $ lein zinc
```

It triggers compilation of scala source and then scala test source. 

You may get java.lang.OutOfMemoryError: PermGen space. It can be workedaround by adding JVM options. 

```
   $ LEIN_JVM_OPTS="-XX:MaxPermSize=256m" lein zinc
```

## Sub tasks

Compile scala and java main source code only. 

    $ lein zinc zinc-compile 

Compile scala and java test source code only. 

    $ lein zinc zinc-test-compile 

Monitor the changes made in main source and compile continuously. Ctrl-C to stop the task. You may want to run 'test-cc' task in a separate terminal as it doesn't compile test source.

    $ lein zinc cc

Monitor the changes made in test source and compile continuously. Ctrl-C to stop the task. You may want to run 'cc' task in a separate terminal as it doesn't compile main source.

    $ lein zinc test-cc


## Customizing the behavior

* Create a profile containing `:zinc-options` map. 
* `:zinc-options` map may contain sub-maps `:logging`, `:inputs`, `:incremental`,
 and/or `:sbt-version`. 
* See "Avaiable Options" below for the complete guide.

```clj
(defproject test-project "0.1.0-SNAPSHOT"
  :description "test project using lein-zinc"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
            ;; specify lein-zinc plugin.
  :plugins [[lein-zinc "0.1.0-SNAPSHOT"]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 ;; scala-library is required and the version specified here 
                 ;; is used to compile if it is not overridden with 'scala-version'
                 ;; option. See below.
                 [org.scala-lang/scala-library "2.11.4"]]
  ;; Specifying "zinc" here triggers scala main/source compilation on invoking
  ;; other tasks.
  :prep-tasks ["zinc" "compile"]
  ;; Most of the options available in zinc compiler are available here. 
  :profiles {:zinc-custom-options 
             {:zinc-options 
              {:logging 
               {:level     "debug"
                :colorize? false}
               :inputs 
                {:sources               ["src/scala" "src/java"] 
                 :test-sources          ["test/scala" "test/java"]
                 :classes               "target/classes"
                 :test-classes          "target/test-classes"
                 :scalac-options        ["-unchecked"]
                 :javac-options         ["-deprecation" "-g"]
                 :analysis-cache        "target/analysis/compile"
                 :test-analysis-cache   "target/analysis/test-compile"
                 :analysis-map          {"src_dir_of_other_project" 
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

            ;; It is possible to override scala and sbt version.
             :custom-scala-version
              {:scala-version "2.10.4"
               :sbt-version "0.13.5"
               :zinc-options 
                {:logging
                 {:level "info"}}}})
```

## Available Options

```
Output options:
  :logging
    :level "level"               Set log level (debug|info|warn|error)
    :color?                      Set color in logging

Compile options:
  :inputs
    :sources ["src_dir"...]      List of scala and java source directories
    :test-sources ["src_dir"...] List of scala/java test source directories   
    :classes "dir"               Destination for compiled classes
    :scalac-options ["opt"...]   Options passed into Scala compiler
    :javac-options ["opt"...]    Options passed into Java compiler
    :compile-order "order"       Compile order for Scala and Java sources
                                 (Mixed|JavaThenScala|ScalaThenJava)
    :analysis-cache "file"       Cache file for compile analysis
    :test-analysis-cache "file"  Cache file for test compile analysis
    :analysis-map {"f" "f",...}  Upstream analysis mapping (file:file,...)

Incremental compiler options:
  :incremental
    :transitive-step <n>         Steps before transitive closure
    :recompile-all-fraction <x>  Limit before recompiling all sources
    :relations-debug?            Enable debug logging of analysis relations
    :api-debug?                  Enable analysis API debugging
    :api-dump-directory "dir"    Destination for analysis API dump
    :api-diff-context-size <n>   Diff context size (in lines) for API debug
    :transactional?              Restore previous class files on failure
    :backup "dir"                Backup location (if transactional)
    :name-hashing?               Enable improved (experimental) incremental 
                                 compilation algorithm

Continuous compiling options:
  :continuous-compile
    :interval-in-ms              Interval to check the changes in source dir

General compiler options;
  :scala-version                 Override scala-library version in project.clj
  :sbt-version                   Default is 0.13.6.
  :fork-java?                    Java compiler runs in a separate process
```

## License

Copyright © 2015 Kenji Nakamura

Distributed under the Eclipse Public License either version 1.0 or any later version.
