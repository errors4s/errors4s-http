package org.errors4s.http4s.circe.middleware.server

import cats.data._
import cats.effect._
import munit._
import org.errors4s.core.syntax.all._
import org.errors4s.http._
import org.errors4s.http.circe._
import org.errors4s.http4s.circe._
import org.http4s._
import org.http4s.headers._

final class CirceHttpErrorToResponseTest extends CatsEffectSuite {
  private def failingHttpRoutesJson(t: Throwable): HttpRoutes[IO] =
    CirceHttpErrorToResponse.json(Sync[IO])(Kleisli(Function.const(OptionT(IO.raiseError[Option[Response[IO]]](t)))))
  private val testRequest: Request[IO] = Request(method = Method.GET)

  test(
    "CirceHttpErrorToResponse.json middleware should yield a application/json+problem with a 501 status when a specific ExtensibleCirceHttpError is raised"
  ) {
    val error: ExtensibleCirceHttpError = ExtensibleCirceHttpError
      .simple(nes"about:blank", nes"Blank Error", HttpStatus(501), None, None)

    failingHttpRoutesJson(error)
      .run(testRequest)
      .value
      .flatMap(
        _.fold(IO[Unit](fail("No Response"))) { resp =>
          IO(assertEquals(resp.status.code, 501)) *>
            IO(
              assertEquals(resp.headers.get[`Content-Type`], Some(`Content-Type`(MediaType.application.`problem+json`)))
            ) *> resp.as[ExtensibleCirceHttpError].flatMap(value => IO(assertEquals(value, error)))
        }
      )
  }

  test(
    "CirceHttpErrorToResponse.json middleware should yield a application/json+problem with a 501 status when a specific ExtensibleCirceHttpProblem is raised"
  ) {
    val error: ExtensibleCirceHttpProblem = ExtensibleCirceHttpProblem
      .simple(Some("about:blank"), Some("Blank Error"), Some(501), None, None)

    failingHttpRoutesJson(error)
      .run(testRequest)
      .value
      .flatMap(
        _.fold(IO[Unit](fail("No Response"))) { resp =>
          IO(assertEquals(resp.status.code, 501)) *>
            IO(
              assertEquals(resp.headers.get[`Content-Type`], Some(`Content-Type`(MediaType.application.`problem+json`)))
            ) *> resp.as[ExtensibleCirceHttpProblem].flatMap(value => IO(assertEquals(value, error)))
        }
      )
  }
}
