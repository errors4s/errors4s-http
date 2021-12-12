package org.errors4s.http4s

package object client {

  /** A client response error the body of which is always considered to be
    * text.
    */
  type ClientResponseErrorTextBody = ClientResponseError[String]
}
