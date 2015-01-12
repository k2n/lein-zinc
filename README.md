# lein-zinc

A Leiningen plugin to compile scala and java source code with [Typesafe zinc](https://github.com/typesafehub/zinc), which is a stand-alone version of scala incremental compiler forked from sbt. 

[![Circle CI](https://circleci.com/gh/k2n/lein-zinc.svg?style=svg)](https://circleci.com/gh/k2n/lein-zinc)
[![Dependencies Status](http://jarkeeper.com/k2n/lein-zinc/status.svg)](http://jarkeeper.com/k2n/lein-zinc)

## The latest version

[![Clojars Project](http://clojars.org/lein-zinc/latest-version.svg)](http://clojars.org/lein-zinc)

## Usage

Put scala source code under `src/scala`, and test code under `test/scala`.

Put `[lein-zinc "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your
`:user` profile.

Add `[org.scala-lang/scala-library "your_scala_version"]` to `:dependencies`.

To automically run zinc compiler in regular lifecycle of leiningen, add `["zinc"]` to `:prep-tasks`. 

Alternatively, run the task directly from the command line.  

    $ lein zinc

It triggers compilation of scala source, and then scala test source. 

## Sub tasks

Compile scala main source code only. 

    $ lein zinc zinc-compile 

Compile scala test source code only. 

    $ lein zinc zinc-test-compile 

## License

Copyright © 2014 Kenji Nakamura

Distributed under the Eclipse Public License either version 1.0 or any later version.
