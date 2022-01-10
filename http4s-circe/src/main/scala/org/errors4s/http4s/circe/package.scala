package org.errors4s.http4s

import cats.effect._
import cats.syntax.all._
import io.circe._
import io.circe.syntax._
import org.errors4s.http.circe._
import org.errors4s.http.{circe => _, _}
import org.http4s._
import org.http4s.circe._
import org.http4s.headers._

package object circe {
  implicit def circeHttpProblemJsonEntityEncoder[F[_]]: EntityEncoder[F, ExtensibleCirceHttpProblem] =
    EntityEncoder[F, Json]
      .contramap((value: ExtensibleCirceHttpProblem) => value.asJson)
      .withContentType(`Content-Type`(MediaType.application.`problem+json`))

  implicit def circeHttpProblemJsonEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, ExtensibleCirceHttpProblem] =
    EntityDecoder.decodeBy(MediaType.application.`problem+json`)(media =>
      jsonOf[F, ExtensibleCirceHttpProblem].decode(media, false)
    )

  implicit def circeHttpErrorJsonEntityEncoder[F[_]]: EntityEncoder[F, ExtensibleCirceHttpError] =
    EntityEncoder[F, Json]
      .contramap((value: ExtensibleCirceHttpError) => value.asJson)
      .withContentType(`Content-Type`(MediaType.application.`problem+json`))

  implicit def circeHttpErrorJsonEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, ExtensibleCirceHttpError] =
    EntityDecoder
      .decodeBy(MediaType.application.`problem+json`)(media => jsonOf[F, ExtensibleCirceHttpError].decode(media, false))

  def httpErrorJsonEntityEncoder[F[_]]: EntityEncoder[F, HttpError] =
    circeHttpErrorJsonEntityEncoder[F].contramap(ExtensibleCirceHttpError.fromHttpError)

  def httpErrorJsonEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, HttpError] = circeHttpErrorJsonEntityDecoder.widen

  def httpProblemJsonEntityEncoder[F[_]]: EntityEncoder[F, HttpProblem] =
    circeHttpProblemJsonEntityEncoder[F].contramap(value => ExtensibleCirceHttpProblem.fromHttpProblem(value))

  def httpProblemJsonEntityDecoder[F[_]: Concurrent]: EntityDecoder[F, HttpProblem] =
    circeHttpProblemJsonEntityDecoder[F].widen

  /** Encode a `HttpError` as a http4s `Response.
    *
    * @note This respects `ExtensibleCirceHttpError` so if you have custom
    *       keys in your `HttpError` then ensure it extends
    *       `ExtensibleCirceHttpError`, otherwise they will not be detected.
    */
  def httpErrorAsResponse[F[_]](e: HttpError)(implicit F: Sync[F]): F[Response[F]] =
    F.fromEither(Status.fromInt(e.status.value))
      .flatMap(status =>
        e match {
          case e: ExtensibleCirceHttpError =>
            F.pure(Response(status = status).withEntity(e)(circeHttpErrorJsonEntityEncoder[F]))
          case _ =>
            F.pure(Response(status = status).withEntity(e)(httpErrorJsonEntityEncoder[F]))
        }
      )

  /** Encode a `HttpProblem` as a http4s `Response.
    *
    * @note This respects `ExtensibleCirceHttpProblem` so if you have custom
    *       keys in your `HttpProblem` then ensure it extends
    *       `ExtensibleCirceHttpProblem`, otherwise they will not be detected.
    */
  def httpProblemAsResponse[F[_]](e: HttpProblem)(implicit F: Sync[F]): F[Response[F]] =
    F.fromEither(
        e.status.fold(Right(Status.InternalServerError): Either[ParseFailure, Status])(value => Status.fromInt(value))
      )
      .flatMap(status =>
        e match {
          case e: ExtensibleCirceHttpProblem =>
            F.pure(Response(status = status).withEntity(e)(circeHttpProblemJsonEntityEncoder[F]))
          case e: HttpProblem =>
            F.pure(Response(status = status).withEntity(e)(httpProblemJsonEntityEncoder[F]))
        }
      )
}
