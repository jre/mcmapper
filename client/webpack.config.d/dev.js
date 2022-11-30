if ('devServer' in config) {
    config.devServer.host = "0.0.0.0"
    config.devServer.allowedHosts = "all"
    config.devServer.open = false
    config.devServer.hot = false
    config.devServer.liveReload = false
}
