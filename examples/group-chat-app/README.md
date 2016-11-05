# group-chat-app

An example of using pneumatic-tubes in React Native application

## Running the example

1. Install re-natal `npm -g i re-natal`
1. Install NPM dependencies `re-natal deps`
1. Run server app from group-chat example in separate terminal `java -Dport=3000 -jar target/group-chat-standalone.jar`
1. Build and run React Native app:
    ```
    $ lein perod biuld
    $ react-native run-ios

    ```
1. Browse the [locahost:3000](http://localhost:3000)

After this you should be able to chat from your browser and iOS app

## License

Copyright © 2016 Artur Girenko

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
