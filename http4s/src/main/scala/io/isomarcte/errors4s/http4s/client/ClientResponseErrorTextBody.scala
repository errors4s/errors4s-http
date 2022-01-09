package org.errors4s.http4s.client

import cats._
import cats.effect.{MonadThrow => _, _}
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
  def fromOptionRequestResponseWithConfigSync[F[_]: Sync](
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
  def fromRequestResponseWithConfigSync[F[_]: Sync](
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
  def fromRequestResponseSync[F[_]: Sync](request: Request[F])(response: Response[F]): F[ClientResponseErrorTextBody] =
    fromRequestResponseWithDecoder[F](EntityDecoder.text[F])(request)(response)

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
  def fromResponseWithConfigSync[F[_]: Sync](config: RedactionConfiguration)(
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
  def fromResponse[F[_]: Sync](response: Response[F]): F[ClientResponseErrorTextBody] =
    fromResponseWithDecoder[F](EntityDecoder.text[F])(response)
}
