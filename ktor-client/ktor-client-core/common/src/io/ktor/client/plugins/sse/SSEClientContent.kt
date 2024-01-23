/*
 * Copyright 2014-2023 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.client.plugins.sse

import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.sse.*
import io.ktor.utils.io.*
import kotlin.time.*

@InternalAPI
public class SSEClientContent(
    public val reconnectionTime: Duration,
    public val showCommentEvents: Boolean,
    public val showRetryEvents: Boolean,
    public val requestBody: OutgoingContent = EmptyContent
) : OutgoingContent.WriteChannelContent() {
    override val contentType: ContentType? = requestBody.contentType

    override suspend fun writeTo(channel: ByteWriteChannel) {
        when (requestBody) {
            is NoContent -> return
            is ByteArrayContent -> channel.writeFully(requestBody.bytes())
            is ReadChannelContent -> requestBody.readFrom().copyAndClose(channel)
            is WriteChannelContent -> requestBody.writeTo(channel)
            is ProtocolUpgrade -> throw SSEException("Unsupported request body: $requestBody")
        }
    }

    override val headers: Headers = HeadersBuilder().apply {
        append(HttpHeaders.Accept, ContentType.Text.EventStream)
        append(HttpHeaders.CacheControl, "no-store")
    }.build()

    override fun toString(): String = "SSEClientContent"
}
