package shipreq.webapp.base.data

import japgolly.nyaya.util.Multimap
import shipreq.base.util.UnivEq
import shipreq.webapp.base.text.Text
import shipreq.webapp.base.text.Text.Equality._

/**
 * Data attributed to requirements beyond their basic definitions.
 */
object ReqData {

  type Text = Map[CustomField.Text.Id, Map[ReqId, Text.CustomTextField.NonEmptyText]]

  def emptyText: Text = Map.empty

  implicit def equalityText: UnivEq[Text] = UnivEq.map

  // -------------------------------------------------------------------------------------------------------------------

  type Tags = Multimap[ReqId, Set, ApplicableTagId]

  def emptyTags: Tags = Multimap.empty

  implicit def equalityTags: UnivEq[Tags] = UnivEq.multimap
}
