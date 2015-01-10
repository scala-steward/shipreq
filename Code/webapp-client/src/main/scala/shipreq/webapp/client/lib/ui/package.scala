package shipreq.webapp.client.lib

import japgolly.scalajs.react.ReactElement
import scalaz.effect.IO

package object ui extends EditorExt {

  type SimpleEditor[I] = SimpleEditor2[I, I]

  type SimpleEditor2[A, B] = Editor[A, B, IO, Unit, Unit, IO[Unit], ReactElement]
}