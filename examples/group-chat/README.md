# group-chat

A [re-frame](https://github.com/Day8/re-frame) multi-room chat application to demonstrate how to:

* selectively dispatching pushing events to a group of clients
* using middleware for server-side handlers
* react on changes in [Datomic](http://www.datomic.com) database.

## Development Mode

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build

```
lein clean
lein cljsbuild once min
```
