package org.errors4s.http4s.client

import _root_.org.errors4s.core._
import _root_.org.errors4s.core.syntax.all._
import cats._
import cats.data._
import cats.implicits._
import org.errors4s.http4s.RedactionConfiguration._
import org.errors4s.http4s._
import org.http4s._

/** Error type which can be used in the `org.http4s.client.Client.expectOr` or
  * `org.http4s.client.Client.expectOptionOr` methods of a http4s Client.
  *
  * There are ''many'' different factory methods one may use to construct a
  * [[ClientResponseError]]. This is because depending on your use case, you
  * may or may not have/want to include the request or you may or may not want
  * to use the default redaction configuration. Some good options to use if
  * you aren't really sure what you want are
  * [[ClientResponseError#fromRequestResponseWithConfig]] and
  * [[ClientResponseError#fromRequestResponseWithConfigWithBody]] in the
  * companion object from `ClientResponseError`. They provide a good trade off
  * of ease of use while still collecting lots of error information. If you
  * wish to merely treat the error body as text, you might consider
  * [[ClientResponseErrorTextBody#fromRequestResponseWithConfig]] as well.
  *
  * @see [[https://http4s.org/v0.21/api/org/http4s/client/client Http4s Client]]
  */
sealed trait ClientResponseError[A] extends Error {

  /** The status of the response which generated this error.
    */
  def status: Status

  /** The redacted request headers from the response which generated this error,
    * if this error had access to the request.
    */
  def requestHeaders: Option[RedactedRequestHeaders]

  /** The HTTP method of the request which generated this error, if this error
    * had access to the request.
    */
  def requestMethod: Option[Method]

  /** The redacted URI of the request which generated this error, if this error
    * had access to the URI.
    */
  def requestUri: Option[RedactedUri]

  /** The redacted response headers from the response which generated this
    * error.
    */
  def responseHeaders: RedactedResponseHeaders

  /** The body of the response which generated this error if present, or if the
    * body was present but failed decoding, then the error from the decoding
    * failure.
    */
  def responseBodyT: EitherT[Option, Throwable, A]

  // protected //

  /** A method which will conditional convert the body of the response into a
    * String.
    */
  protected def showResponseBody: A => Option[String]

  // final //

  /** Whether or not the error response which generated this error had a body.
    */
  final def errorResponseHadBody: Boolean = responseBody.isDefined

  /** The body of the response which generated this error if both the body was
    * present and it was able to be converted into a String.
    *
    * @note If you'd like to distinguish between a missing body and a failure
    *       to convert a present body into a String, you should use either
    *       [[#responseBodyT]] or [[#responseBody]].
    */
  final def responseBodyText: Option[String] =
    responseBodyT.foldF(Function.const(Option.empty[String]), (a: A) => showResponseBody(a))

  /** As [[#responseBodyT]], but does not use transformers.
    */
  final def responseBody: Option[Either[Throwable, A]] = responseBodyT.value

  /** If this error had access to the request headers, then this is the redacted
    * request headers as the canonical http4s type.
    */
  final def requestHeadersValue: Option[Headers] = requestHeaders.map(_.value)

  /** The response headers, in the canonical http4s type.
    */
  final def responseHeadersValue: Headers = responseHeaders.value

  final def map[B](f: A => B, showResponseBody: B => Option[String]): ClientResponseError[B] =
    ClientResponseError(
      status,
      requestHeaders,
      requestMethod,
      requestUri,
      responseHeaders,
      responseBodyT.map(f),
      showResponseBody
    )

  // Overrides //

  final override def primaryErrorMessage: NonEmptyString =
    requestUri
      .flatMap(_.value.host.map(host => nes"Unexpected response from HTTP call to ${host.renderString}: ${status}"))
      .getOrElse(nes"Unexpected response from HTTP call: ${status}")

  final override def secondaryErrorMessages: Vector[String] =
    requestUri.map(uri => s"Request URI: ${uri.value.renderString}").toVector ++
      requestMethod.map(method => s"Request Method: ${method.renderString}").toVector ++ Vector(s"Status: ${status}") ++
      Vector(s"Response Headers: ${responseHeaders.value}") ++
      requestHeaders.map(headers => s"Request Headers: ${headers.value}").toVector ++
      responseBodyText.fold(Vector.empty[String])(value => Vector(s"Response Body: ${value.toString}"))

  final override def causes: Vector[Throwable] =
    responseBodyT
      .foldF(
        (t: Throwable) =>
          Some(Error.withMessageAndCause(nes"Error occurred when attempting to decode the error response body.", t)),
        Function.const(None)
      )
      .toVector

  final override def toString: String = s"ClientResponseError(${getLocalizedMessage})"
}

object ClientResponseError {

  final private[this] case class ClientResponseErrorImpl[A](
    override val status: Status,
    override val requestHeaders: Option[RedactedRequestHeaders],
    override val requestMethod: Option[Method],
    override val requestUri: Option[RedactedUri],
    override val responseHeaders: RedactedResponseHeaders,
    override val responseBodyT: EitherT[Option, Throwable, A],
    override protected val showResponseBody: A => Option[String]
  ) extends ClientResponseError[A]

  /** The lowest level constructor for a [[ClientResponseError]].
    *
    * @note Unless you have very specialized use cases, you should ''not'' use
    *       this constructor and prefer the other factory methods in either
    *       [[ClientResponseError]], [[ClientResponseErrorTextBody]], or
    *       [[ClientResponseErrorNoBody]].
    *
    * @param status The HTTP status of the response which generated this
    *               error.
    * @param requestHeaders Optionally, the redacted request headers for the
    *                       request which generated this error.
    * @param requestMethod Optionally, the method on the request which
    *                      generated this error.
    * @param requestUri Optionally, the redacted uri from the request which
    *                   generated this error.
    * @param responseHeaders The redacted response headers from the response
    *                        which generated this error.
    * @param responseBodyT The result of attempting to decode the body of the
    *                      response which generated this error, if there
    *                      was a body, and or nothing if there was no body.
    * @param showResponseBody A function to optionally convert the response
    *                         body into String. If the result is None, it will
    *                         be assumed the response body is not renderable
    *                         as a String for some reason, such as sensitive
    *                         data or just convenience.
    */
  def apply[A](
    status: Status,
    requestHeaders: Option[RedactedRequestHeaders],
    requestMethod: Option[Method],
    requestUri: Option[RedactedUri],
    responseHeaders: RedactedResponseHeaders,
    responseBodyT: EitherT[Option, Throwable, A],
    showResponseBody: A => Option[String]
  ): ClientResponseError[A] =
    ClientResponseErrorImpl[A](
      status,
      requestHeaders,
      requestMethod,
      requestUri,
      responseHeaders,
      responseBodyT,
      showResponseBody
    )

  /** Create a [[ClientResponseError]] from a redaction configuration, a
    * function to conditionally show the response, an explicitly defined
    * EntityDecoder, optionally the request, and the response.
    *
    * Idiomatic usage would be to partially apply the first parameter list
    * then use the result like so.
    *
    * {{{
    * scala> val clientErrorHandler: Option[Request[IO]] => Response[IO] => IO[ClientResponseError[String]]  =
    *   ClientResponseError.fromOptionRequestResponseWithConfigWithBody[IO, String](
    *     RedactionConfiguration.default,
    *     (_: String) => None,
    *     EntityDecoder.text
    *   )
    *
    * scala> client.expectOr[String](request)(clientErrorHandler(Some(request))).attempt.unsafeRunSync()
    * val res0: Either[Throwable,String] = Left(ClientResponseError(Primary Error: Unexpected response from HTTP call to localhost: 400 Bad Request, Secondary Errors(Request URI: http://localhost:8080, Request Method: GET, Status: 400 Bad Request, Response Headers: Headers(), Request Headers: Headers())))
    * }}}
    *
    * @note This is one of the most general ways to construct a
    *       [[ClientResponseError]]. For most use cases, one of the other
    *       factory methods may be easier to use.
    */
  def fromOptionRequestResponseWithConfigWithBody[F[_], A](
    config: RedactionConfiguration,
    showResponseBody: A => Option[String],
    entityDecoder: EntityDecoder[F, A]
  )(
    request: Option[Request[F]]
  )(response: Response[F])(implicit F: MonadError[F, Throwable]): F[ClientResponseError[A]] = {
    val buildError: EitherT[Option, Throwable, A] => ClientResponseError[A] =
      (
        decodeResult =>
          ClientResponseError(
            response.status,
            request.map(value => RedactedRequestHeaders.fromRequestAndConfig(value, config)),
            request.map(_.method),
            request.map(value => RedactedUri.fromRequestAndConfig(value, config)),
            RedactedResponseHeaders.fromResponseAndConfig(response, config),
            decodeResult,
            a => showResponseBody(a)
          )
      )

    response
      .as[A](F, entityDecoder)
      .redeem((t: Throwable) => buildError(EitherT.leftT(t)), (a: A) => buildError(EitherT.rightT(a)))
  }

  /** Create a [[ClientResponseError]] from a redaction configuration, an
    * optionally the request, and the response.
    *
    * This is similar to [[#fromOptionRequestResponseWithConfigWithBody]], but
    * never attempts to decode the error body.
    *
    * Idiomatic usage would be to partially apply the first parameter list
    * then use the result like so.
    *
    * {{{
    * scala> val clientErrorHandler: Option[Request[IO]] => Response[IO] => ClientResponseError[Nothing]  =
    *   ClientResponseError.fromOptionRequestResponseWithConfig[IO](RedactionConfiguration.default)
    *
    * scala> client.expectOr[String](request)(errorResponse => IO.pure(clientErrorHandler(Some(request))(errorResponse))).attempt.unsafeRunSync()
    * client.expectOr[String](request)(errorResponse => IO.pure(clientErrorHandler(Some(request))(errorResponse))).attempt.unsafeRunSync()val res2: Either[Throwable,String] = Left(ClientResponseError(Primary Error: Unexpected response from HTTP call to localhost: 400 Bad Request, Secondary Errors(Request URI: http://localhost:8080, Request Method: GET, Status: 400 Bad Request, Response Headers: Headers(), Request Headers: Headers())))
    * }}}
    *
    * @note This is one of the most general ways to construct a
    *       [[ClientResponseError]]. For most use cases, one of the other
    *       factory methods may be easier to use.
    */
  def fromOptionRequestResponseWithConfig[F[_]](
    config: RedactionConfiguration
  )(request: Option[Request[F]])(response: Response[F]): ClientResponseError[Nothing] = {
    ClientResponseError[Nothing](
      response.status,
      request.map(value => RedactedRequestHeaders.fromRequestAndConfig(value, config)),
      request.map(_.method),
      request.map(value => RedactedUri.fromRequestAndConfig(value, config)),
      RedactedResponseHeaders.fromResponseAndConfig(response, config),
      EitherT[Option, Throwable, Nothing](None),
      Function.const(None)
    )
  }

  /** As [[#fromOptionRequestResponseWithConfig]], but assumes the `Request`
    * will always be present.
    */
  def fromRequestResponseWithConfig[F[_]](config: RedactionConfiguration)(request: Request[F])(
    response: Response[F]
  ): ClientResponseError[Nothing] = fromOptionRequestResponseWithConfig[F](config)(Some(request))(response)

  /** As [[#fromRequestResponseWithConfig]], but uses
    * [[RedactionConfiguration#default]].
    */
  def fromRequestResponse[F[_]](request: Request[F])(response: Response[F]): ClientResponseError[Nothing] =
    fromRequestResponseWithConfig[F](RedactionConfiguration.default)(request)(response)

  /** As [[#fromOptionRequestResponseWithConfig]], but assumes there will never
    * be a `Request` available for error generation.
    */
  def fromResponseWithConfig[F[_]](config: RedactionConfiguration)(
    response: Response[F]
  ): ClientResponseError[Nothing] = fromOptionRequestResponseWithConfig[F](config)(None)(response)

  /** As [[#fromResponseWithConfig]], but uses
    * [[RedactionConfiguration#default]].
    */
  def fromResponse[F[_]](response: Response[F]): ClientResponseError[Nothing] =
    fromResponseWithConfig[F](RedactionConfiguration.default)(response)

  /** As [[#fromOptionRequestResponseWithConfigWithBody]], but assumes the
    * `Request` will always be available for error generation.
    */
  def fromRequestResponseWithConfigWithBody[F[_]: MonadThrow, A](
    config: RedactionConfiguration,
    showResponseBody: A => Option[String],
    entityDecoder: EntityDecoder[F, A]
  )(request: Request[F])(response: Response[F]): F[ClientResponseError[A]] =
    fromOptionRequestResponseWithConfigWithBody[F, A](config, showResponseBody, entityDecoder)(Some(request))(response)

  /** As [[#fromRequestResponseWithBody]], but uses
    * [[RedactionConfiguration#default]].
    */
  def fromRequestResponseWithBody[F[_]: MonadThrow, A](
    showResponseBody: A => Option[String],
    entityDecoder: EntityDecoder[F, A]
  )(request: Request[F])(response: Response[F]): F[ClientResponseError[A]] =
    fromRequestResponseWithConfigWithBody[F, A](RedactionConfiguration.default, showResponseBody, entityDecoder)(
      request
    )(response)

  /** As [[#fromOptionRequestResponseWithConfigWithBody]], but assumes the
    * `Request` will never be present for error generation.
    */
  def fromResponseWithConfigWithBody[F[_]: MonadThrow, A](
    config: RedactionConfiguration,
    showResponseBody: A => Option[String],
    entityDecoder: EntityDecoder[F, A]
  )(response: Response[F]): F[ClientResponseError[A]] =
    fromOptionRequestResponseWithConfigWithBody[F, A](config, showResponseBody, entityDecoder)(None)(response)

  /** As [[#fromResponseWithConfigWithBody]], but uses
    * [[RedactionConfiguration#default]].
    */
  def fromResponseWithBody[F[_]: MonadThrow, A](
    showResponseBody: A => Option[String],
    entityDecoder: EntityDecoder[F, A]
  )(response: Response[F]): F[ClientResponseError[A]] =
    fromResponseWithConfigWithBody[F, A](RedactionConfiguration.default, showResponseBody, entityDecoder)(response)
}
