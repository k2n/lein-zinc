# lein-zinc plugin test project

This is a project to test lein-zinc plugin.

## Usage
* Invoke `./lein install` in the parent directory to compile and install lein-zinc plugin in local maven repository. 
* Invoke `./lein zinc` to compile java and scala source code. 
* Invoke `./lein zinc cc` to compile source code continuously. 
* Invoke `./lein zinc test-cc` to compile test source code continuously. 
* Invoke `./lein with-profiles zinc-custom-options zinc` to test custom properties. 
* Invoke `./lein with-profiles custom-scala-version zinc` to use different sbt/scala version.

## Debug
* Invoke `DEBUG=true ./lein zinc` to display debug level messages.
* 
## License

Copyright © 2015 Kenji Nakamura

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
