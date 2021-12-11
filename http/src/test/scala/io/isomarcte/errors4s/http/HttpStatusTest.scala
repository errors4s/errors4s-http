package org.errors4s.http

import munit._

final class HttpStatusTest extends FunSuite {

  test("Creating compile time valid HttpStatus values should compile correctly") {
    assertEquals(HttpStatus(100), HttpStatus.unsafe(100))
  }
}
