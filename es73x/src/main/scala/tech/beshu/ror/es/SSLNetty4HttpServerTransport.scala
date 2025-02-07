/*
 *    This file is part of ReadonlyREST.
 *
 *    ReadonlyREST is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    ReadonlyREST is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with ReadonlyREST.  If not, see http://www.gnu.org/licenses/
 */
package tech.beshu.ror.es

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

import io.netty.buffer.ByteBufAllocator
import io.netty.channel.Channel
import io.netty.handler.ssl.{ClientAuth, NotSslRecordException, SslContext, SslContextBuilder}
import org.apache.logging.log4j.scala.Logging
import org.elasticsearch.common.network.NetworkService
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.util.BigArrays
import org.elasticsearch.common.xcontent.NamedXContentRegistry
import org.elasticsearch.http.netty4.Netty4HttpServerTransport
import org.elasticsearch.http.{HttpChannel, HttpServerTransport}
import org.elasticsearch.threadpool.ThreadPool
import tech.beshu.ror.configuration.SslConfiguration
import tech.beshu.ror.es.utils.AccessControllerHelper.doPrivileged
import tech.beshu.ror.utils.SSLCertParser

import scala.collection.JavaConverters._

class SSLNetty4HttpServerTransport(settings: Settings,
                                   networkService: NetworkService,
                                   bigArrays: BigArrays,
                                   threadPool: ThreadPool,
                                   xContentRegistry: NamedXContentRegistry,
                                   dispatcher: HttpServerTransport.Dispatcher,
                                   ssl: SslConfiguration)
  extends Netty4HttpServerTransport(settings, networkService, bigArrays, threadPool, xContentRegistry, dispatcher)
    with Logging {

  override def onException(channel: HttpChannel, cause: Exception): Unit = {
    if (!this.lifecycle.started) return
    else if (cause.getCause.isInstanceOf[NotSslRecordException]) logger.warn(cause.getMessage + " connecting from: " + channel.getRemoteAddress)
    else super.onException(channel, cause)
    channel.close()
  }

  override def configureServerChannelHandler = new SSLHandler(this)

  final class SSLHandler(transport: Netty4HttpServerTransport)
    extends Netty4HttpServerTransport.HttpChannelHandler(transport, handlingSettings) {

    private var context = Option.empty[SslContext]

    doPrivileged {
      SSLCertParser.run(new SSLContextCreatorImpl, ssl)
    }

    override def initChannel(ch: Channel): Unit = {
      super.initChannel(ch)
      context.foreach { sslCtx =>
        ch.pipeline().addFirst("ror_internode_ssl_handler", sslCtx.newHandler(ch.alloc()))
      }
    }

    private class SSLContextCreatorImpl extends SSLCertParser.SSLContextCreator {
      override def mkSSLContext(certChain: String, privateKey: String): Unit = {
        try { // #TODO expose configuration of sslPrivKeyPem password? Letsencrypt never sets one..
          val sslCtxBuilder = SslContextBuilder.forServer(
            new ByteArrayInputStream(certChain.getBytes(StandardCharsets.UTF_8)),
            new ByteArrayInputStream(privateKey.getBytes(StandardCharsets.UTF_8)),
            null
          )
          logger.info("ROR SSL HTTP: Using SSL provider: " + SslContext.defaultServerProvider.name)
          SSLCertParser.validateProtocolAndCiphers(sslCtxBuilder.build.newEngine(ByteBufAllocator.DEFAULT), ssl)
          if (ssl.allowedCiphers.nonEmpty) sslCtxBuilder.ciphers(ssl.allowedCiphers.map(_.value).toList.asJava)
          if (ssl.verifyClientAuth) sslCtxBuilder.clientAuth(ClientAuth.REQUIRE)
          if (ssl.allowedProtocols.nonEmpty) sslCtxBuilder.protocols(ssl.allowedProtocols.map(_.value).toList: _*)
          context = Some(sslCtxBuilder.build)
        } catch {
          case e: Exception =>
            context = None
            logger.error("Failed to load SSL HTTP CertChain & private key from Keystore! " + e.getClass.getSimpleName + ": " + e.getMessage, e)
        }
      }
    }
  }
}
