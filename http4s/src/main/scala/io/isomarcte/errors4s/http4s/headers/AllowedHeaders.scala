package org.errors4s.http4s.headers

import cats.data._
import cats.syntax.all._
import org.typelevel.ci._

/** Headers which are known to have safe values for logging.
  *
  * @see [[https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers]]
  */
object AllowedHeaders {

  // Private //

  private val defaultAllowAuthenticationHeaders: NonEmptySet[String] = NonEmptySet
    .of("WWW-Authenticate", "Proxy-Authenticate")

  private val defaultAllowCachingHeaders: NonEmptySet[String] = NonEmptySet
    .of("Age", "Cache-Control", "Clear-Site-Data", "Expires", "Pragma", "Warning")

  private val defaultAllowClientHintsHeaders: NonEmptySet[String] = NonEmptySet
    .of("Accept-CH", "Accept-CH-Liftetime", "Early-Data", "Device-Memory", "Save-Data", "Viewport-Width", "Width")

  private val defaultAllowConditionalsHeaders: NonEmptySet[String] = NonEmptySet
    .of("Last-Modified", "ETag", "If-Match", "If-None-Match", "If-Modified-Since", "If-Unmodified-Since", "Vary")

  private val defaultAllowConnectionManagementHeaders: NonEmptySet[String] = NonEmptySet.of("Connection", "Keep-Alive")

  private val defaultAllowContentNegotiationHeaders: NonEmptySet[String] = NonEmptySet
    .of("Accept", "Accept-Charset", "Accept-Encoding", "Accept-Language")

  private val defaultAllowControlsHeaders: NonEmptySet[String] = NonEmptySet.of("Expect", "Max-Forwards")

  private val defaultAllowCORSHeaders: NonEmptySet[String] = NonEmptySet.of(
    "Access-Control-Allow-Origin",
    "Access-Control-Allow-Credentials",
    "Access-Control-Allow-Headers",
    "Access-Control-Expose-Methods",
    "Access-Control-Max-Age",
    "Access-Control-Request-Headers",
    "Access-Control-Request-Method",
    "Origin",
    "Timing-Allow-Origin"
  )

  private val defaultAllowDoNotTrackHeaders: NonEmptySet[String] = NonEmptySet.of("DNT", "Tk")

  private val defaultAllowDownloadHeaders: NonEmptySet[String] = NonEmptySet.of("Content-Disposition")

  private val defaultAllowMessageBodyInformationHeaders: NonEmptySet[String] = NonEmptySet
    .of("Content-Length", "Content-Type", "Content-Encoding", "Content-Language", "Content-Location")

  private val defaultAllowProxiesHeaders: NonEmptySet[String] = NonEmptySet
    .of("Forwarded", "X-Forwarded-For", "X-Forwarded-Host", "X-Forwarded-Proto", "Via")

  private val defaultAllowRedirectsHeaders: NonEmptySet[String] = NonEmptySet.of("Location")

  private val defaultAllowRequestContextHeaders: NonEmptySet[String] = NonEmptySet
    .of("From", "Host", "Referer", "Referer-Policy", "User-Agent")

  private val defaultAllowResponseContextHeaders: NonEmptySet[String] = NonEmptySet.of("Allow", "Server")

  private val defaultAllowRangeRequestsHeaders: NonEmptySet[String] = NonEmptySet
    .of("Accept-Ranges", "Range", "If-Range", "Content-Range")

  private val defaultAllowSecurityHeaders: NonEmptySet[String] = NonEmptySet.of(
    "Cross-Origin-Embedder-Policy",
    "Cross-Origin-Opener-Policy",
    "Cross-Origin-Resource-Policy",
    "Content-Security-Policy",
    "Content-Security-Policy-Report-Only",
    "Expect-CT",
    "Feature-Policy",
    "Strict-Transport-Security",
    "X-Content-Type-Options",
    "X-Download-Options",
    "X-Frame-Options",
    "X-Permitted-Cross-Domain-Policies",
    "X-Powered-By",
    "X-XSS-Protection"
  )

  private val defaultAllowHPKPHeaders: NonEmptySet[String] = NonEmptySet
    .of("Public-Key-Pins", "Public-Key-Pins-Report-Only")

  private val defaultAllowFetchMetadataRequestHeaders: NonEmptySet[String] = NonEmptySet
    .of("Sec-Fetch-Site", "Sec-Fetch-Mode", "Sec-Fetch-User", "Sec-Fetch-Dest")

  private val defaultAllowTransferCodingHeaders: NonEmptySet[String] = NonEmptySet
    .of("Transfer-Encoding", "TE", "Trailer")

  private val defaultAllowOtherHeaders: NonEmptySet[String] = NonEmptySet.of(
    "Alt-Svc",
    "Date",
    "Large-Allocation",
    "Link",
    "Retry-After",
    "Server-Timing",
    "SourceMap",
    "X-SourceMap",
    "Upgrade",
    "X-DNS-Prefetch-Control"
  )

  // Public //

  /** The set of headers which are not redacted when using the default
    * [[org.errors4s.http4s.RedactionConfiguration#default]].
    */
  val defaultAllowHeaders: NonEmptySet[CIString] =
    (
      defaultAllowAuthenticationHeaders ++ defaultAllowCachingHeaders ++ defaultAllowClientHintsHeaders ++
        defaultAllowConditionalsHeaders ++ defaultAllowConnectionManagementHeaders ++
        defaultAllowContentNegotiationHeaders ++ defaultAllowControlsHeaders ++ defaultAllowCORSHeaders ++
        defaultAllowDoNotTrackHeaders ++ defaultAllowDownloadHeaders ++ defaultAllowMessageBodyInformationHeaders ++
        defaultAllowProxiesHeaders ++ defaultAllowRedirectsHeaders ++ defaultAllowRequestContextHeaders ++
        defaultAllowResponseContextHeaders ++ defaultAllowRangeRequestsHeaders ++ defaultAllowSecurityHeaders ++
        defaultAllowHPKPHeaders ++ defaultAllowFetchMetadataRequestHeaders ++ defaultAllowTransferCodingHeaders ++
        defaultAllowOtherHeaders
    ).map(value => CIString(value))
}
