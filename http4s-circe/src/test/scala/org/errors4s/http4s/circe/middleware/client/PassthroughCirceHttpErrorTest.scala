package org.errors4s.http4s.circe.middleware.client

import cats.effect._
import cats.implicits._
import fs2.Chunk
import fs2.Stream
import io.circe._
import io.circe.syntax._
import java.nio.charset.StandardCharsets
import munit._
import org.errors4s.core._
import org.errors4s.core.circe.instances._
import org.errors4s.http._
import org.errors4s.http.circe._
import org.errors4s.http.circe.implicits.httpStatusCodec
import org.errors4s.http4s.circe._
import org.http4s._
import org.http4s.client._
import org.http4s.headers._
import scodec.bits.ByteVector

final class PassthroughCirceHttpErrorTest extends CatsEffectSuite {
  import PassthroughCirceHttpErrorTest._

  private def bodyAsChunk(value: Response[IO]): IO[Chunk[Byte]] =
    value.body.chunks.foldMonoid(Chunk.instance.algebra[Byte]).compile.lastOrError

  private def compareResponses(value: Response[IO], expected: Response[IO]): IO[Unit] =
    IO(assertEquals(value.status, expected.status)) *> IO(assertEquals(value.headers, expected.headers)) *>
      bodyAsChunk(value)
        .flatMap(value => bodyAsChunk(expected).flatMap(expected => IO(assertEquals(value, expected)).void))

  test(
    "A response with a application/problem+json Content-Type should raise an error in F if the body is well formed"
  ) {
    val client: Client[IO] = constClientF(httpErrorAsResponse[IO](testError))

    client
      .run(testRequest)
      .use[Unit](resp => IO(fail(s"Expected error, but got $resp")))
      .attemptT
      .foldF(
        e =>
          e match {
            case e: ExtensibleCirceHttpProblem =>
              IO(assertEquals(e.asJson, testError.asJson))
            case otherwise =>
              IO(fail(s"Expected ExtensibleCirceHttpProblem, but got $otherwise"))
          },
        value => IO(fail(s"Expected error, but got successful $value"))
      )
  }

  test(
    "A response with a application/problem+json Content-Type should yield the original response if parsing fails (e.g. the Content-Type is wrong)"
  ) {
    val client: Client[IO] = constClient(responseWithContentType)
    client.run(testRequest).use[Unit](resp => compareResponses(resp, responseWithContentType))
  }

  test(
    "The PassthroughCirceHttpError middleware should only read the body if there is an application/problem+json, and in that case it should only do so once"
  ) {
    Ref
      .of[IO, Int](0)
      .flatMap { ref =>
        val responseBody: String = "Body"
        val response: Response[IO] = Response(
          status = Status.Ok,
          headers = Headers.apply(`Content-Type`(MediaType.application.`problem+json`)),
          body = Stream.evalUnChunk(
            ref.update(_ + 1) *>
              IO.pure(Chunk.byteVector(ByteVector.view(responseBody.getBytes(StandardCharsets.UTF_8))))
          )
        )

        val client: Client[IO] = constClient(response)

        // The result should be _2_ and not _1_ because the body is evaluated
        // once in the middleware and once again by the `compareResponses`
        // helper (which uses the original `fs2.Stream` value.
        client.run(testRequest).use(value => compareResponses(value, response)) *>
          ref.get.flatMap(value => IO(assertEquals(value, 2)))
      }
  }

  test(
    "The PassthroughCirceHttpError middleware should not read the body at all if the content-type is not an application/problem+json"
  ) {
    Ref
      .of[IO, Int](0)
      .flatMap { ref =>
        val responseBody: String = "Body"
        val response: Response[IO] = Response(
          status = Status.Ok,
          body = Stream.evalUnChunk(
            ref.update(_ + 1) *>
              IO.pure(Chunk.byteVector(ByteVector.view(responseBody.getBytes(StandardCharsets.UTF_8))))
          )
        )

        val client: Client[IO] = constClient(response)

        client
          .run(testRequest)
          .use(value =>
            // Throw away the body, which should prevent the Ref from ever being
            // updated if the middleware is working correctly.
            IO.pure(value.copy(body = Stream.empty))
          ) *> ref.get.flatMap(value => IO(assertEquals(value, 0)))
      }
  }
}

object PassthroughCirceHttpErrorTest {
  final private case class CustomCirceHttpError(
    override val `type`: NonEmptyString,
    override val title: NonEmptyString,
    override val status: HttpStatus,
    override val detail: Option[String],
    override val instance: Option[NonEmptyString],
    customInt: Int
  ) extends ExtensibleCirceHttpError {
    override lazy val additionalFields: JsonObject = JsonObject("customInt" -> customInt.asJson)
  }

  private object CustomCirceHttpError {
    implicit lazy val c: Codec[CustomCirceHttpError] =
      Codec.forProduct6("type", "title", "status", "detail", "instance", "customInt")(CustomCirceHttpError.apply _)(
        (value: CustomCirceHttpError) =>
          (value.`type`, value.title, value.status, value.detail, value.instance, value.customInt)
      )
  }

  private lazy val testError: CustomCirceHttpError = CustomCirceHttpError(
    NonEmptyString("about:blank"),
    NonEmptyString("Title"),
    HttpStatus(501),
    None,
    None,
    1
  )

  private lazy val testRequest: Request[IO] = Request()

  private lazy val responseWithContentType: Response[IO] = Response(headers =
    Headers(`Content-Type`(MediaType.application.`problem+json`))
  )

  private def constClientF(response: IO[Response[IO]]): Client[IO] =
    PassthroughCirceHttpError(Sync[IO])(Client[IO](Function.const(Resource.eval(response))))

  private def constClient(response: Response[IO]): Client[IO] = constClientF(IO.pure(response))
}
