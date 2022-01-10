package org.errors4s.http4s

import cats.syntax.all._
import org.errors4s.http4s.headers._
import org.http4s._
import org.http4s.util._
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

  /** Do not redact headers with names appearing in the given sets and query
    * parameter keys appearing in the given set. If the header or query
    * parameter name is not in the given set, then fallback to the
    * [[#default]].
    *
    * @note No query parameter keys will be redacted, just values.
    *
    * @note Unlike HTTP header values, URI query parameter keys technically
    *       ''are'' case sensitive, but usually they are treated as though
    *       they are case insensitive. If you want case sensitive redaction,
    *       then do not use this method.
    */
  def allowListOrDefaultCI(
    allowedRequestHeaderNames: Set[CaseInsensitiveString],
    allowedResponseHeaderNames: Set[CaseInsensitiveString],
    allowedQueryParameterKeys: Set[CaseInsensitiveString]
  ): RedactionConfiguration =
    default
      .withRedactRequestHeader(RedactRequestHeader.allowListOrDefaultCI(allowedRequestHeaderNames))
      .withRedactResponseHeader(RedactResponseHeader.allowListOrDefaultCI(allowedResponseHeaderNames))
      .withRedactUriQueryParam(RedactUriQueryParam.allowListOrDefaultCI(allowedQueryParameterKeys))

  /** Do not redact headers with names appearing in the given sets and query
    * parameter keys appearing in the given set. If the header or query
    * parameter name is not in the given set, then fallback to the
    * [[#default]].
    *
    * @note No query parameter keys will be redacted, just values.
    *
    * @note Unlike HTTP header values, URI query parameter keys technically
    *       ''are'' case sensitive, but usually they are treated as though
    *       they are case insensitive. If you want case sensitive redaction,
    *       then do not use this method.
    */
  def allowListOrDefault(
    allowedRequestHeaderNames: Set[String],
    allowedResponseHeaderNames: Set[String],
    allowedQueryParameterKeys: Set[String]
  ): RedactionConfiguration =
    allowListOrDefaultCI(
      allowedRequestHeaderNames.map(CaseInsensitiveString.apply),
      allowedResponseHeaderNames.map(CaseInsensitiveString.apply),
      allowedQueryParameterKeys.map(CaseInsensitiveString.apply)
    )

  /** Do not redact headers with names appearing in the given sets and query
    * parameter keys appearing in the given set. If the header or query
    * parameter name is not in the given set, then fallback to the
    * [[#default]].
    *
    * @note The same set of header names is used for both request and response
    *       headers.
    *
    * @note No query parameter keys will be redacted, just values.
    *
    * @note Unlike HTTP header values, URI query parameter keys technically
    *       ''are'' case sensitive, but usually they are treated as though
    *       they are case insensitive. If you want case sensitive redaction,
    *       then do not use this method.
    */
  def allowListOrDefaultCI(
    allowedHeaderNames: Set[CaseInsensitiveString],
    allowedQueryParameterKeys: Set[CaseInsensitiveString]
  ): RedactionConfiguration = allowListOrDefaultCI(allowedHeaderNames, allowedHeaderNames, allowedQueryParameterKeys)

  /** Do not redact headers with names appearing in the given sets and query
    * parameter keys appearing in the given set. If the header or query
    * parameter name is not in the given set, then fallback to the
    * [[#default]].
    *
    * @note The same set of header names is used for both request and response
    *       headers.
    *
    * @note No query parameter keys will be redacted, just values.
    *
    * @note Unlike HTTP header values, URI query parameter keys technically
    *       ''are'' case sensitive, but usually they are treated as though
    *       they are case insensitive. If you want case sensitive redaction,
    *       then do not use this method.
    */
  def allowListOrDefault(
    allowedHeaderNames: Set[String],
    allowedQueryParameterKeys: Set[String]
  ): RedactionConfiguration = allowListOrDefault(allowedHeaderNames, allowedHeaderNames, allowedQueryParameterKeys)

  // Newtype related private functions and values //

  private[this] def headerInAllowedHeaders(value: Header): Boolean =
    AllowedHeaders.defaultAllowHeaders.contains(value.name)

  // Newtypes, because a lot of these functions are of the same type.

  /** A newtype for a function to redact request header values.
    */
  final case class RedactRequestHeader(value: Header => Header) extends AnyVal

  object RedactRequestHeader {

    /** The default [[RedactRequestHeader]]. It will redact the value of any
      * header not in
      * [[org.errors4s.http4s.headers.AllowedHeaders#defaultAllowHeaders]].
      */
    val default: RedactRequestHeader = RedactRequestHeader(value =>
      if (headerInAllowedHeaders(value)) {
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

    /** Redact headers using the given partial function falling back to an
      * alternative redaction method.
      */
    def orElse(pf: PartialFunction[Header, Header], fallback: RedactRequestHeader): RedactRequestHeader =
      RedactRequestHeader(header => pf.applyOrElse(header, fallback.value(_)))

    /** Redact headers using the given partial function falling back to the
      * default redaction method.
      */
    def orElseDefault(pf: PartialFunction[Header, Header]): RedactRequestHeader = orElse(pf, default)

    /** Do not redact headers with names appearing in the given set, but redact
      * all other headers.
      */
    def allowListCI(headerNames: Set[CaseInsensitiveString]): RedactRequestHeader =
      RedactRequestHeader(header =>
        if (headerNames.contains(header.name)) {
          header
        } else {
          Header.Raw(header.name, defaultRedactValue(header.value))
        }
      )

    /** Do not redact headers with names appearing in the given set, but redact
      * all other headers.
      */
    def allowList(headerNames: Set[String]): RedactRequestHeader =
      allowListCI(headerNames.map(CaseInsensitiveString.apply))

    /** Do not redact headers with names appearing in the given set. If the header
      * name is not in the given set, then fallback to the [[#default]]
      * redaction method.
      */
    def allowListOrDefaultCI(headerNames: Set[CaseInsensitiveString]): RedactRequestHeader =
      RedactRequestHeader(header =>
        if (headerNames.contains(header.name)) {
          header
        } else {
          default.value(header)
        }
      )

    /** Do not redact headers with names appearing in the given set. If the header
      * name is not in the given set, then fallback to the [[#default]]
      * redaction method.
      */
    def allowListOrDefault(headerNames: Set[String]): RedactRequestHeader =
      allowListOrDefaultCI(headerNames.map(CaseInsensitiveString.apply))
  }

  /** A newtype for a function to redact response header values.
    */
  final case class RedactResponseHeader(value: Header => Header) extends AnyVal

  object RedactResponseHeader {

    /** The default [[RedactResponseHeader]]. It will redact the value of any
      * header not in
      * [[org.errors4s.http4s.headers.AllowedHeaders#defaultAllowHeaders]].
      */
    val default: RedactResponseHeader = RedactResponseHeader(value =>
      if (headerInAllowedHeaders(value)) {
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

    /** Redact headers using the given partial function falling back to an
      * alternative redaction method.
      */
    def orElse(pf: PartialFunction[Header, Header], fallback: RedactResponseHeader): RedactResponseHeader =
      RedactResponseHeader(header => pf.applyOrElse(header, fallback.value(_)))

    /** Redact headers using the given partial function falling back to the
      * default redaction method.
      */
    def orElseDefault(pf: PartialFunction[Header, Header]): RedactResponseHeader = orElse(pf, default)

    /** Do not redact headers with names appearing in the given set, but redact
      * all other headers.
      */
    def allowListCI(headerNames: Set[CaseInsensitiveString]): RedactResponseHeader =
      RedactResponseHeader(header =>
        if (headerNames.contains(header.name)) {
          header
        } else {
          Header.Raw(header.name, defaultRedactValue(header.value))
        }
      )

    /** Do not redact headers with names appearing in the given set, but redact
      * all other headers.
      */
    def allowList(headerNames: Set[String]): RedactResponseHeader =
      allowListCI(headerNames.map(CaseInsensitiveString.apply))

    /** Do not redact headers with names appearing in the given set. If the header
      * name is not in the given set, then fallback to the [[#default]]
      * redaction method.
      */
    def allowListOrDefaultCI(headerNames: Set[CaseInsensitiveString]): RedactResponseHeader =
      RedactResponseHeader(header =>
        if (headerNames.contains(header.name)) {
          header
        } else {
          default.value(header)
        }
      )

    /** Do not redact headers with names appearing in the given set. If the header
      * name is not in the given set, then fallback to the [[#default]]
      * redaction method.
      */
    def allowListOrDefault(headerNames: Set[String]): RedactResponseHeader =
      allowListOrDefaultCI(headerNames.map(CaseInsensitiveString.apply))
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

    /** Redact query parameter using the given partial function falling back to an
      * alternative redaction method.
      */
    def orElse(
      pf: PartialFunction[(String, Option[String]), (String, Option[String])],
      fallback: RedactUriQueryParam
    ): RedactUriQueryParam =
      RedactUriQueryParam((key, value) =>
        pf.applyOrElse(key -> value, (kv: (String, Option[String])) => fallback.value(kv._1, kv._2))
      )

    /** Redact query parameter using the given partial function falling back to
      * the default redaction method.
      */
    def orElseDefault(pf: PartialFunction[(String, Option[String]), (String, Option[String])]): RedactUriQueryParam =
      orElse(pf, default)

    /** Do not redact query parameter values with the case insensitive name
      * appearing in the given set, but redact all other query parameter
      * values.
      *
      * @note No query parameter keys will be redacted, just values.
      *
      * @note Unlike HTTP header values, URI query parameter keys technically
      *       ''are'' case sensitive, but usually they are treated as though
      *       they are case insensitive. If you want case sensitive redaction,
      *       then do not use this method.
      */
    def allowListCI(queryParamKeys: Set[CaseInsensitiveString]): RedactUriQueryParam =
      RedactUriQueryParam {
        case (key, Some(value)) if queryParamKeys.contains(CaseInsensitiveString(key)) =>
          key -> Some(value)
        case (key, Some(value)) =>
          key -> Some(defaultRedactValue(value))
        case (key, _) =>
          key -> None
      }

    /** Do not redact query parameter values with the case insensitive name
      * appearing in the given set, but redact all other query parameter
      * values.
      *
      * @note No query parameter keys will be redacted, just values.
      *
      * @note Unlike HTTP header values, URI query parameter keys technically
      *       ''are'' case sensitive, but usually they are treated as though
      *       they are case insensitive. If you want case sensitive redaction,
      *       then do not use this method.
      */
    def allowList(queryParamKeys: Set[String]): RedactUriQueryParam =
      allowListCI(queryParamKeys.map(CaseInsensitiveString.apply))

    /** Do not redact query parameter values with a case insensitive name
      * appearing in the given set. If the query parameter key name is not in
      * the given set, then fallback to the [[#default]] redaction method.
      *
      * @note No query parameter keys will be redacted, just values.
      *
      * @note Unlike HTTP header values, URI query parameter keys technically
      *       ''are'' case sensitive, but usually they are treated as though
      *       they are case insensitive. If you want case sensitive redaction,
      *       then do not use this method.
      */
    def allowListOrDefaultCI(queryParamKeys: Set[CaseInsensitiveString]): RedactUriQueryParam =
      RedactUriQueryParam {
        case (key, Some(value)) if queryParamKeys.contains(CaseInsensitiveString(value)) =>
          key -> Some(value)
        case (key, Some(value)) =>
          key -> Some(defaultRedactValue(value))
        case (key, _) =>
          key -> None
      }

    /** Do not redact query parameter values with a case insensitive name
      * appearing in the given set. If the query parameter key name is not in
      * the given set, then fallback to the [[#default]] redaction method.
      *
      * @note No query parameter keys will be redacted, just values.
      *
      * @note Unlike HTTP header values, URI query parameter keys technically
      *       ''are'' case sensitive, but usually they are treated as though
      *       they are case insensitive. If you want case sensitive redaction,
      *       then do not use this method.
      */
    def allowListOrDefault(queryParamKeys: Set[String]): RedactUriQueryParam =
      allowListOrDefaultCI(queryParamKeys.map(CaseInsensitiveString.apply))
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
        value = headers.foldMap(header => Headers.of(redact.value(header))),
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

    /** Create a [[RedactedResponseHeaders]] value from unredacted headers and a
      * [[RedactResponseHeader]].
      *
      * @note Normally this is done for you when generating a
      *       [[org.errors4s.http4s.client.ClientResponseError]].
      */
    def fromHeaders(headers: Headers, redact: RedactResponseHeader): RedactedResponseHeaders =
      RedactedResponseHeadersImpl(
        value = headers.foldMap(header => Headers.of(redact.value(header))),
        unredacted = headers
      )

    /** Create a [[RedactedResponseHeaders]] value from a `Response` value.
      *
      * @note Normally this is done for you when generating a
      *       [[org.errors4s.http4s.client.ClientResponseError]].
      */
    def fromResponse[F[_]](response: Response[F], redact: RedactResponseHeader): RedactedResponseHeaders =
      fromHeaders(response.headers, redact)

    /** Create a [[RedactedResponseHeaders]] value from unredacted request headers
      * and a [[RedactionConfiguration]].
      *
      * @note Normally this is done for you when generating a
      *       [[org.errors4s.http4s.client.ClientResponseError]].
      */
    def fromHeadersAndConfig(headers: Headers, config: RedactionConfiguration): RedactedResponseHeaders =
      fromHeaders(headers, config.redactResponseHeader)

    /** Create a [[RedactedResponseHeaders]] value from a `Response` value
      * and a [[RedactionConfiguration]].
      *
      * @note Normally this is done for you when generating a
      *       [[org.errors4s.http4s.client.ClientResponseError]].
      */
    def fromResponseAndConfig[F[_]](response: Response[F], config: RedactionConfiguration): RedactedResponseHeaders =
      fromHeaders(response.headers, config.redactResponseHeader)
  }

  /** Uri values which been redacted.
    */
  sealed trait RedactedUri {

    /** The redacted Uri.
      */
    def value: Uri

    /** The original, unredacted, Uri.
      */
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

    /** Create a [[RedactedUri]] value from an unredacted Uri and a
      * [[RedactUriQueryParam]].
      *
      * @note Normally this is done for you when generating a
      *       [[org.errors4s.http4s.client.ClientResponseError]].
      */
    def fromUri(uri: Uri, redact: RedactUriQueryParam): RedactedUri = RedactedUriImpl(uri, redact)

    /** Create a [[RedactedUri]] value from `Request` and a
      * [[RedactUriQueryParam]].
      *
      * @note Normally this is done for you when generating a
      *       [[org.errors4s.http4s.client.ClientResponseError]].
      */
    def fromRequest[F[_]](request: Request[F], redact: RedactUriQueryParam): RedactedUri = fromUri(request.uri, redact)

    /** Create a [[RedactedUri]] value from a Uri and a
      * [[RedactionConfiguration]].
      *
      * @note Normally this is done for you when generating a
      *       [[org.errors4s.http4s.client.ClientResponseError]].
      */
    def fromUriAndConfig(uri: Uri, config: RedactionConfiguration): RedactedUri =
      fromUri(uri, config.redactUriQueryParam)

    /** Create a [[RedactedUri]] value from a `Request` and a
      * [[RedactionConfiguration]].
      *
      * @note Normally this is done for you when generating a
      *       [[org.errors4s.http4s.client.ClientResponseError]].
      */
    def fromRequestAndConfig[F[_]](request: Request[F], config: RedactionConfiguration): RedactedUri =
      fromUri(request.uri, config.redactUriQueryParam)
  }

  // General utility functions, exposed so users have an easy time modifying
  // the default behavior.

  /** The default function used to redact a value. It redacts all values with
    * the string "<REDACTED>".
    */
  def defaultRedactValue[A](value: A): String = redactWithConstantString[A]("<REDACTED>")(value)

  /** Redact a value using a constant string. */
  def redactWithConstantString[A](constant: String)(@nowarn("cat=unused") value: A): String = constant
}
