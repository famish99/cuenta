# cuenta

A [re-frame](https://github.com/Day8/re-frame) application designed to tally up debts between your friends.

## Development Mode

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3460](http://localhost:3460).

Additionally, to run the full-stack in live-reload mode, start the server using the following:

```
lein repl
=> (use 'backend)
=> (main)
```

Will launch the backend in dev mode, which will var-quote the handlers such that they'll always be reread and evaluated.

## Production Build

To run the production server:

```
lein clean
lein with-profile prod run
```
