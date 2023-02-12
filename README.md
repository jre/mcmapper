# mcmapper

A minecraft mapping experiment written in compose multiplatform. It
reads minecraft player-created maps and joins them together into a
mosaic which is viewable in a browser. A Nether subway route overlay
can be shown as well, if route and stop data has been configured.

Prebuild development snapshots of the
[backend tool](https://nightly.link/jre/mcmapper/workflows/build.yaml/master/mcmapper-backend.zip)
and
[web client](https://nightly.link/jre/mcmapper/workflows/build.yaml/master/mcmapper-web.zip)
are available. There is also a
[desktop client](https://nightly.link/jre/mcmapper/workflows/build.yaml/master/mcmapper-desktop.zip)
and an
[android client](https://nightly.link/jre/mcmapper/workflows/build.yaml/master/mcmapper-android-debug.apk.zip).

### To build the backend tool

Run `./gradlew shadowJar` and copy `mcmapper-backend.jar` and your
backend JSON configs to your minecraft server. Look in the
`example-backend-config/` directory for an example config.

### To build the webapp

Run `./gradlew jsBrowserWebpack` then copy the contents of the `js/`
directory to your web root.

### To use the webapp

To use the webapp you will need to periodically run the backend tool
on the minecraft server. It will read your minecraft maps and write
the resulting JSON and PNG data into the web root. If your web root
is, for example, `/var/www/html/` then you might want to copy the webapp
into `/var/www/html/mc/` and the map data into `/var/www/html/mc/data/`

`java -jar mcmapper-backend.jar convert-map /path/to/my/worlds.json /var/www/html/mc/data/`

### To develop the webapp

Copy `client/webpack.config.d/proxy.js.example` to
`client/webpack.config.d/proxy.js` and edit the proxy url to point to
the data directory on the webserver where `mcmapper-backend.jar`
runs. Run `./gradlew jsBrowserDevelopmentRun` to build.
