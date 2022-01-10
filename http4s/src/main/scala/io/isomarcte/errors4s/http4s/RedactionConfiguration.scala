package org.errors4s.http4s

import cats.syntax.all._
import org.errors4s.http4s.headers._
import org.http4s._
import org.typelevel.ci._
import scala.annotation.nowarn

/** This type describes how to redact a request/response interaction when
  * constructing a [[org.errors4s.http4s.client.ClientResponseError]].
  *
  * @note Any redaction ''can'' be undone. The redacted wrapper type for each
  *       redaction provides public access to the unredacted value. This value
  *       does not appear in the toString, but can be recovered if
  *       needed. Obviously, care should be taken when doing this.
  */
sealed trait RedactionConfiguration {
  import RedactionConfiguration._

  /** A redaction function applied to the request headers.
    */
  def redactRequestHeader: RedactRequestHeader

  /** A redaction function applied to the response headers.
    */
  def redactResponseHeader: RedactResponseHeader

  /** A redaction function applied to the query parameters in the uri.
    */
  def redactUriQueryParam: RedactUriQueryParam

  // final //

  /** Set the [[#redactRequestHeader]]. */
  final def withRedactRequestHeader(f: RedactRequestHeader): RedactionConfiguration =
    RedactionConfiguration.withRedactRequestHeader(this)(f)

  /** Set the [[#redactResponseHeader]]. */
  final def withRedactResponseHeader(f: RedactResponseHeader): RedactionConfiguration =
    RedactionConfiguration.withRedactResponseHeader(this)(f)

  /** Set the [[#redactUriQueryParam]]. */
  final def withRedactUriQueryParam(f: RedactUriQueryParam): RedactionConfiguration =
    RedactionConfiguration.withRedactUriQueryParam(this)(f)

  final override def toString: String = s"RedactionConfiguration(hashCode = ${this.hashCode})"
}

object RedactionConfiguration {

  final private[this] case class RedactionConfigurationImpl(
    override val redactRequestHeader: RedactRequestHeader,
    override val redactResponseHeader: RedactResponseHeader,
    override val redactUriQueryParam: RedactUriQueryParam
  ) extends RedactionConfiguration

  /** The default [[RedactionConfiguration]].
    *
    * This uses [[#RedactRequestHeader#default]],
    * [[#RedactResponseHeader#default]], and
    * [[#RedactUriQueryParam#default]].
    *
    * This configuration will redact the value of any request or response
    * headers which do not appear in
    * [[org.errors4s.http4s.headers.AllowedHeaders#defaultAllowHeaders]]. The
    * default redaction function for the uri query parameters replaces all
    * values with the constant string "<REDACTED>", but leaves the keys alone.
    */
  lazy val default: RedactionConfiguration = RedactionConfigurationImpl(
    RedactRequestHeader.default,
    RedactResponseHeader.default,
    RedactUriQueryParam.default
  )

  /** This [[RedactionConfiguration]] will not do ''any'' redaction. Each of the
    * redaction functions are just the identity function.
    */
  lazy val unredacted: RedactionConfiguration = RedactionConfigurationImpl(
    RedactRequestHeader.unredacted,
    RedactResponseHeader.unredacted,
    RedactUriQueryParam.unredacted
  )

  /** Given a [[RedactionConfiguration]] update the [[RedactRequestHeader]]
    * function.
    */
  def withRedactRequestHeader(value: RedactionConfiguration)(f: RedactRequestHeader): RedactionConfiguration =
    value match {
      case value: RedactionConfigurationImpl =>
        value.copy(redactRequestHeader = f)
    }

  /** Given a [[RedactionConfiguration]] update the
    * [[RedactResponseHeader]] function.
    */
  def withRedactResponseHeader(value: RedactionConfiguration)(f: RedactResponseHeader): RedactionConfiguration =
    value match {
      case value: RedactionConfigurationImpl =>
        value.copy(redactResponseHeader = f)
    }

  /** Given a [[RedactionConfiguration]] update the
    * [[RedactUriQueryParam]] function.
    */
  def withRedactUriQueryParam(value: RedactionConfiguration)(f: RedactUriQueryParam): RedactionConfiguration =
    value match {
      case value: RedactionConfigurationImpl =>
        value.copy(redactUriQueryParam = f)
    }

  // Newtype related private functions and values //

  private[this] def headerInAllowedHeaders(value: CIString): Boolean =
    AllowedHeaders.defaultAllowHeaders.contains(value)

  // Newtypes, because a lot of these functions are of the same type.

  /** A newtype for a function to redact request header values.
    */
  final case class RedactRequestHeader(value: Header.Raw => Header.Raw) extends AnyVal

  object RedactRequestHeader {

    /** The default [[RedactRequestHeader]]. It will redact the value of any
      * header not in
      * [[org.errors4s.http4s.headers.AllowedHeaders#defaultAllowHeaders]].
      */
    val default: RedactRequestHeader = RedactRequestHeader(value =>
      if (headerInAllowedHeaders(value.name)) {
        value
      } else {
        Header.Raw(value.name, defaultRedactValue(value.value))
      }
    )

    /** A redaction function which does not redact anything.
      *
      * A common use case for this would be when you want to have all the
      * request header values unredacted, but the response header values or
      * the uri query parameter values redacted.
      */
    val unredacted: RedactRequestHeader = RedactRequestHeader(identity)
  }

  /** A newtype for a function to redact response header values.
    */
  final case class RedactResponseHeader(value: Header.Raw => Header.Raw) extends AnyVal

  object RedactResponseHeader {

    /** The default [[RedactResponseHeader]]. It will redact the value of any
      * header not in
      * [[org.errors4s.http4s.headers.AllowedHeaders#defaultAllowHeaders]].
      */
    val default: RedactResponseHeader = RedactResponseHeader(value =>
      if (headerInAllowedHeaders(value.name)) {
        value
      } else {
        Header.Raw(value.name, defaultRedactValue(value.value))
      }
    )

    /** A redaction function which does not redact anything.
      *
      * A common use case for this would be when you want to have all the
      * response header values unredacted, but the request header values or
      * the uri query parameter values redacted.
      */
    val unredacted: RedactResponseHeader = RedactResponseHeader(identity)
  }

  /** A newtype for a function to redact uri query parameter keys and values.
    */
  final case class RedactUriQueryParam(value: (String, Option[String]) => (String, Option[String])) extends AnyVal

  object RedactUriQueryParam {

    /** The default [[RedactUriQueryParam]]. It will redact all the values with
      * the string "<REDACTED>" and leave all keys unmodified.
      */
    val default: RedactUriQueryParam = RedactUriQueryParam {
      case (key, Some(value)) =>
        (key, Some(redactWithConstantString("REDACTED")(value)))
      case (key, None) =>
        (key, None)
    }

    /** A redaction function which does not redact anything.
      *
      * A common use case for this would be when you want to have all the uri
      * query parameter values unredacted, but the response header values or
      * the request values redacted.
      */
    val unredacted: RedactUriQueryParam = RedactUriQueryParam((key, value) => (key, value))
  }

  // Redacted newtypes, these are the results of applying the Redact newtype functions

  /** Request header values which have been redacted.
    */
  sealed trait RedactedRequestHeaders {

    /** The redacted headers.
      */
    def value: Headers

    /** The original, unredacted, headers.
      */
    def unredacted: Headers

    // final //

    final override def toString: String = s"RedactedRequestHeaders(value = ${value})"
  }

  object RedactedRequestHeaders {
    final private[this] case class RedactedRequestHeadersImpl(
      override val value: Headers,
      override val unredacted: Headers
    ) extends RedactedRequestHeaders

    /** Create a [[RedactedRequestHeaders]] value from unredacted headers and a
      * [[RedactRequestHeader]].
      *
      * @note Normally this is done for you when generating a
      *       [[org.errors4s.http4s.client.ClientResponseError]].
      */
    def fromHeaders(headers: Headers, redact: RedactRequestHeader): RedactedRequestHeaders =
      RedactedRequestHeadersImpl(
        value = Headers(headers.headers.foldMap(header => List(redact.value(header))): List[Header.Raw]),
        unredacted = headers
      )

    /** Create a [[RedactedRequestHeaders]] value from a `Request` value.
      *
      * @note Normally this is done for you when generating a
      *       [[org.errors4s.http4s.client.ClientResponseError]].
      */
    def fromRequest[F[_]](request: Request[F], redact: RedactRequestHeader): RedactedRequestHeaders =
      fromHeaders(request.headers, redact)

    /** Create a [[RedactedRequestHeaders]] value from unredacted request headers
      * and a [[RedactionConfiguration]].
      *
      * @note Normally this is done for you when generating a
      *       [[org.errors4s.http4s.client.ClientResponseError]].
      */
    def fromHeadersAndConfig(headers: Headers, config: RedactionConfiguration): RedactedRequestHeaders =
      fromHeaders(headers, config.redactRequestHeader)

    /** Create a [[RedactedRequestHeaders]] value from a `Request` value
      * and a [[RedactionConfiguration]].
      *
      * @note Normally this is done for you when generating a
      *       [[org.errors4s.http4s.client.ClientResponseError]].
      */
    def fromRequestAndConfig[F[_]](request: Request[F], config: RedactionConfiguration): RedactedRequestHeaders =
      fromHeadersAndConfig(request.headers, config)
  }

  /** Response header values which have been redacted.
    */
  sealed trait RedactedResponseHeaders {

    /** The redacted headers.
      */
    def value: Headers

    /** The original, unredacted, headers.
      */
    def unredacted: Headers

    // final //

    final override def toString: String = s"RedactedResponseHeaders(value = ${value})"
  }

  object RedactedResponseHeaders {
    final private[this] case class RedactedResponseHeadersImpl(
      override val value: Headers,
      override val unredacted: Headers
    ) extends RedactedResponseHeaders

    def fromHeaders(headers: Headers, redact: RedactResponseHeader): RedactedResponseHeaders =
      RedactedResponseHeadersImpl(
        value = Headers(headers.headers.foldMap(header => List(redact.value(header))): List[Header.Raw]),
        unredacted = headers
      )

    def fromResponse[F[_]](response: Response[F], redact: RedactResponseHeader): RedactedResponseHeaders =
      fromHeaders(response.headers, redact)

    def fromHeadersAndConfig(headers: Headers, config: RedactionConfiguration): RedactedResponseHeaders =
      fromHeaders(headers, config.redactResponseHeader)

    def fromResponseAndConfig[F[_]](response: Response[F], config: RedactionConfiguration): RedactedResponseHeaders =
      fromHeaders(response.headers, config.redactResponseHeader)
  }

  sealed trait RedactedUri {
    def value: Uri

    def unredacted: Uri

    // final //

    final override def toString: String = s"RedcatedUri(value = ${value})"
  }

  object RedactedUri {
    final private[this] case class RedactedUriImpl(override val unredacted: Uri, redactionF: RedactUriQueryParam)
        extends RedactedUri {
      override def value: Uri =
        unredacted.copy(query =
          Query.fromVector(
            unredacted
              .query
              .pairs
              .foldMap { case (key, value) =>
                Vector(redactionF.value(key, value))
              }
          )
        )
    }

    def fromUri(uri: Uri, redact: RedactUriQueryParam): RedactedUri = RedactedUriImpl(uri, redact)

    def fromRequest[F[_]](request: Request[F], redact: RedactUriQueryParam): RedactedUri = fromUri(request.uri, redact)

    def fromUriAndConfig(uri: Uri, config: RedactionConfiguration): RedactedUri =
      fromUri(uri, config.redactUriQueryParam)

    def fromRequestAndConfig[F[_]](request: Request[F], config: RedactionConfiguration): RedactedUri =
      fromUri(request.uri, config.redactUriQueryParam)
  }

  // General utility functions, exposed so users have an easy time modifying
  // the default behavior.

  def defaultRedactValue[A](value: A): String = redactWithConstantString[A]("<REDACTED>")(value)

  def redactWithConstantString[A](constant: String)(
    @nowarn
    value: A
  ): String = constant
}
