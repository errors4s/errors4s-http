package org.errors4s.http4s.client

import cats._
import cats.effect._
import org.errors4s.http4s._
import org.http4s._

/** Methods for creating instances of [[ClientResponseError]] which have a
  * text error body.
  */
object ClientResponseErrorTextBody {

  /** As [[ClientResponseError#fromOptionRequestResponseWithConfigWithBody]],
    * but assumes the body will be a `String`.
    */
  def fromOptionRequestResponseWithConfigAndDecoder[F[_]: MonadThrow](
    config: RedactionConfiguration,
    entityDecoder: EntityDecoder[F, String]
  )(request: Option[Request[F]])(response: Response[F]): F[ClientResponseErrorTextBody] =
    ClientResponseError
      .fromOptionRequestResponseWithConfigWithBody[F, String](config, value => Some(value), entityDecoder)(request)(
        response
      )

  /** As [[#fromOptionRequestResponseWithConfigAndDecoder]], but uses the
    * default `EntityDecoder` for `String` values.
    */
  @deprecated(message = "Please use fromOptionRequestResponseWithConfig instead.", since = "3.0.0.0")
  def fromOptionRequestResponseWithConfigSync[F[_]: Concurrent](
    config: RedactionConfiguration
  )(request: Option[Request[F]])(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromOptionRequestResponseWithConfig[F](config)(request)(response)

  /** As [[#fromOptionRequestResponseWithConfigAndDecoder]], but uses the
    * default `EntityDecoder` for `String` values.
    */
  def fromOptionRequestResponseWithConfig[F[_]: Concurrent](
    config: RedactionConfiguration
  )(request: Option[Request[F]])(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromOptionRequestResponseWithConfigAndDecoder[F](config, EntityDecoder.text[F])(request)(response)

  /** As [[#fromOptionRequestResponseWithConfigAndDecoder]], but assumes the
    * request will always be present for error generation.
    */
  def fromRequestResponseWithConfigAndDecoder[F[_]: MonadThrow](
    config: RedactionConfiguration,
    entityDecoder: EntityDecoder[F, String]
  )(request: Request[F])(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromOptionRequestResponseWithConfigAndDecoder[F](config, entityDecoder)(Some(request))(response)

  /** As [[#fromRequestResponseWithConfigAndDecoder]], but uses the default
    * `EntityDecoder` for `String` values.
    */
  @deprecated(message = "Please use fromRequestResponseWithConfig instead.", since = "3.0.0.0")
  def fromRequestResponseWithConfigSync[F[_]: Concurrent](config: RedactionConfiguration)(request: Request[F])(
    response: Response[F]
  ): F[ClientResponseErrorTextBody] = fromRequestResponseWithConfig[F](config)(request)(response)

  /** As [[#fromRequestResponseWithConfigAndDecoder]], but uses the default
    * `EntityDecoder` for `String` values.
    */
  def fromRequestResponseWithConfig[F[_]: Concurrent](
    config: RedactionConfiguration
  )(request: Request[F])(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromRequestResponseWithConfigAndDecoder[F](config, EntityDecoder.text[F])(request)(response)

  /** As [[#fromRequestResponseWithConfigAndDecoder]], but uses
    * [[#RedactionConfiguration#default]].
    */
  def fromRequestResponseWithDecoder[F[_]: MonadThrow](
    entityDecoder: EntityDecoder[F, String]
  )(request: Request[F])(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromRequestResponseWithConfigAndDecoder[F](RedactionConfiguration.default, entityDecoder)(request)(response)

  /** As [[#fromRequestResponseWithDecoder]], but uses the default
    * `EntityDecoder` for `String` values.
    */
  @deprecated(message = "Please use fromRequestResponse instead.", since = "3.0.0.0")
  def fromRequestResponseSync[F[_]: Concurrent](request: Request[F])(
    response: Response[F]
  ): F[ClientResponseErrorTextBody] = fromRequestResponse[F](request)(response)

  /** As [[#fromRequestResponseWithDecoder]], but uses the default
    * `EntityDecoder` for `String` values.
    */
  def fromRequestResponse[F[_]: Concurrent](request: Request[F])(
    response: Response[F]
  ): F[ClientResponseErrorTextBody] = fromRequestResponseWithDecoder[F](EntityDecoder.text[F])(request)(response)

  /** As [[#fromOptionRequestResponseWithConfigAndDecoder]], but assumes the
    * request will never be present for error generation.
    */
  def fromResponseWithConfigAndDecoder[F[_]: MonadThrow](
    config: RedactionConfiguration,
    entityDecoder: EntityDecoder[F, String]
  )(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromOptionRequestResponseWithConfigAndDecoder[F](config, entityDecoder)(None)(response)

  /** As [[#fromResponseWithConfigAndDecoder]], but uses the default
    * `EntityDecoder` for `String` values.
    */
  @deprecated(message = "Please use fromResponseWithConfig instead.", since = "3.0.0.0")
  def fromResponseWithConfigSync[F[_]: Concurrent](config: RedactionConfiguration)(
    response: Response[F]
  ): F[ClientResponseErrorTextBody] = fromResponseWithConfig[F](config)(response)

  /** As [[#fromResponseWithConfigAndDecoder]], but uses the default
    * `EntityDecoder` for `String` values.
    */
  def fromResponseWithConfig[F[_]: Concurrent](config: RedactionConfiguration)(
    response: Response[F]
  ): F[ClientResponseErrorTextBody] = fromResponseWithConfigAndDecoder[F](config, EntityDecoder.text[F])(response)

  /** As [[#fromResponseWithConfigAndDecoder]], but uses
    * [[RedactionConfiguration#default]].
    */
  def fromResponseWithDecoder[F[_]: MonadThrow](
    entityDecoder: EntityDecoder[F, String]
  )(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromResponseWithConfigAndDecoder[F](RedactionConfiguration.default, entityDecoder)(response)

  /** As [[#fromResponseWithDecoder]], but uses the default `EntityDecoder` for
    * `String` values.
    */
  def fromResponse[F[_]: Concurrent](response: Response[F]): F[ClientResponseErrorTextBody] =
    fromResponseWithDecoder[F](EntityDecoder.text[F])(response)
}
