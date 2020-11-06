package shipreq.webapp.base.data

/** A person's full name.
 *
 * E.g. "David Barri", "C K Panipuri".
 *
 * Don't try to reliably extract a given/family name.
 * https://www.w3.org/International/questions/qa-personal-names#fielddesign
 */
final case class PersonName(value: String)

object PersonName {
  implicit def univEq: UnivEq[PersonName] = UnivEq.derive
}
