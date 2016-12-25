package com.timeout.docless

import scala.language.implicitConversions

package object swagger {
  implicit def strToPath(s: String): Path = Path(s)
  implicit def strToResponse(s: String): Responses.Response =
    Responses.Response(
      description = s
    )
}
