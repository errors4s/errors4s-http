package org.errors4s.http4s.circe.middleware.server

import cats.data._
import cats.effect._
import cats.implicits._
import munit._
import org.errors4s.core.syntax.all._
import org.errors4s.http._
import org.errors4s.http.circe._
import org.errors4s.http4s.circe._
import org.http4s._
import org.http4s.headers._

final class CirceHttpErrorToResponseTest extends CatsEffectSuite {
  private def failingHttpRoutesJson(t: Throwable): HttpRoutes[SyncIO] =
    CirceHttpErrorToResponse
      .json(Sync[SyncIO])(Kleisli(Function.const(OptionT(SyncIO.raiseError[Option[Response[SyncIO]]](t)))))
  private val testRequest: Request[SyncIO] = Request(method = Method.GET)

  test(
    "CirceHttpErrorToResponse.json middleware should yield a application/json+problem with a 501 status when a specific ExtensibleCirceHttpError is raised"
  ) {
    val error: ExtensibleCirceHttpError = ExtensibleCirceHttpError
      .simple(nes"about:blank", nes"Blank Error", HttpStatus(501), None, None)

    failingHttpRoutesJson(error)
      .run(testRequest)
      .value
      .flatMap(
        _.fold(SyncIO[Unit](fail("No Response"))) { resp =>
          SyncIO(assertEquals(resp.status.code, 501)) *>
            SyncIO(
              assertEquals(resp.headers.get[`Content-Type`], Some(`Content-Type`(MediaType.application.`problem+json`)))
            ) *> resp.as[ExtensibleCirceHttpError].flatMap(value => SyncIO(assertEquals(value, error)))
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
        _.fold(SyncIO[Unit](fail("No Response"))) { resp =>
          SyncIO(assertEquals(resp.status.code, 501)) *>
            SyncIO(
              assertEquals(resp.headers.get[`Content-Type`], Some(`Content-Type`(MediaType.application.`problem+json`)))
            ) *> resp.as[ExtensibleCirceHttpProblem].flatMap(value => SyncIO(assertEquals(value, error)))
        }
      )
  }
}
