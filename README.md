# lein-zinc

A Leiningen plugin to compile scala and java source code with [Typesafe zinc](https://github.com/typesafehub/zinc), which is a stand-alone version of scala incremental compiler forked from sbt. 

[![Circle CI](https://circleci.com/gh/k2n/lein-zinc.svg?style=svg)](https://circleci.com/gh/k2n/lein-zinc)

## Usage

Put scala source code under `src/scala`.

Put `[lein-zinc "0.1.0-SNAPSHOT"]` into the `:plugins` vector of your
`:user` profile.

Add `[org.scala-lang/scala-library "your_scala_version"]` to `:dependencies`.

To automically run zinc compiler in regular lifecycle of leiningen, add `["zinc"]` to `:prep-tasks`. 

Alternatively, run the task directly from the command line.  

    $ lein zinc

## Options

TBD

## License

Copyright © 2014 Kenji Nakamura

Distributed under the Eclipse Public License either version 1.0 or any later version.
