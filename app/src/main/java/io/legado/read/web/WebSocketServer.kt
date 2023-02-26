package io.legado.read.web

import fi.iki.elonen.NanoWSD
import io.legado.read.service.WebService
import io.legado.read.web.socket.BookSourceDebugWebSocket
import io.legado.read.web.socket.RssSourceDebugWebSocket

class WebSocketServer(port: Int) : NanoWSD(port) {

    override fun openWebSocket(handshake: IHTTPSession): WebSocket? {
        WebService.serve()
        return when (handshake.uri) {
            "/bookSourceDebug" -> {
                BookSourceDebugWebSocket(handshake)
            }
            "/rssSourceDebug" -> {
                RssSourceDebugWebSocket(handshake)
            }
            else -> null
        }
    }
}
