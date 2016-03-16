# pneumatic-tubes

WebSocket based transport of events between re-frame app and server

## Why

While it is quite easy to [talk to servers](https://github.com/Day8/re-frame/wiki/Talking-To-Servers)
in a re-frame app, what if you could just throw `[:your-event data]` from client directly to server,
process it, and dispatch one or more `[:server-side-event with data]` back to one or more client re-frame apps?
I think this is a very intuitive way of building real time features in your re-frame app.

So, the idea of pneumatic-tubes:
* Define handlers for some of your re-frame events on server.
* Dispatch particular events to server via a WebSocket connection.
* Dispatch any other re-frame events from server to one or more clients

## Usage

### Server

```clojure
(:use org.httpkit.server)
(:require [pneumatic-tubes.core :refer [receiver transmitter dispatch]]
          [pneumatic-tubes.httpkit :refer [websocket-handler]])

(def tx (transmitter))          ;; responsible for transmitting messages to one or more clients

(def rx                         ;; collection of handlers for processing incoming messages
  (receiver
    {:say-hello                 ;; re-frame event name
     (fn [from [_ name]]        ;; re-frame event handler function
       (println "Hello" name)
       (dispatch tx from [:say-hello-processed])  ;; send event to same 'tube' where :say-hello came from
       from)}))

(def handler (websocket-handler rx))   ;; kttp-kit based WebSocket request handler
                                       ;; it also works with Compojure routes

(run-server handler {:port 9090})
```
### Client

```clojure
  (:require [re-frame.core :as re-frame]
            [pneumatic-tubes.core :as tubes]

(defn on-receive [event-v]                                        ;; handler of incoming events from server
  (.log js/console "received from server:" (str event-v))
  (re-frame/dispatch event-v))

(def tube (tubes/tube (str "ws://localhost:9090/ws") on-receive)) ;; definition of event 'tube' over WebSocket
(def send-to-server (tubes/send-to-tube-middleware tube))         ;; middleware to send event to server
                                                                  ;; after it is processed on client

(re-frame/register-handler                      ;; normal re-frame handler
  :say-hello
  send-to-server                                ;; forwards this event also to server
  (fn [db [_ name]]
    (.log js/console (str "Hello " name))
    db))

(re-frame/register-handler                      ;; will be called by server
  :say-hello-processed
  (fn [db _]
    (.log js/console "Yay!!!")
    db))

(tubes/create! tube)
```

## License

Copyright © 2016 Artūr Girenko

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
