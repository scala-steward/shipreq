package shipreq.webapp.snippet

import net.liftweb.util.Helpers._
import shipreq.base.util.ScalaExt._
import shipreq.prop.util._
import shipreq.webapp.base.protocol._
import shipreq.webapp.lib.ServerProtocol
import shipreq.webapp.base.data._
import shipreq.webapp.base.data.delta._
import DeletionAction._
import shipreq.webapp.util.QuietException

class WIP {

  def newProject = {
    import shipreq.webapp.base.data._
    import shipreq.webapp.base.UnsafeTypes._

    val customImplTypes = DataSet[CustomIncmpType](10, List(
      CustomIncmpType(1, "TODO", "Something you need To Do.", Alive),
      CustomIncmpType(2, "TBD", "To Be Decided.", Alive)))

    val customReqTypes = DataSet[CustomReqType](20, List(
        CustomReqType(1, "CO", Set.empty, "Constraint", ImplicationNotRequired, Alive),
        CustomReqType(2, "MF", Set.empty, "Major Feature", ImplicationNotRequired, Alive),
        CustomReqType(3, "FR", Set.empty, "Functional Requirement", ImplicationRequired, Alive),
        CustomReqType(4, "BR", Set.empty, "Business Rule", ImplicationNotRequired, Alive),
        CustomReqType(5, "DD", Set("DA", "DDF"), "Data Definition", ImplicationNotRequired, Dead),
        CustomReqType(6, "SI", Set.empty, "Solution Idea", ImplicationRequired, Dead)))

    val tags = RevAnd(30, TagTree(
      Tag.IdAccess mapById List(
        TagGroup(1, "Priority", None, IsEnumLike, Alive),
        ApplicableTag(2, "High Priority", None, "pri=high", Alive),
        ApplicableTag(3, "Medium Priority", None, "pri=med", Alive),
        ApplicableTag(4, "Low Priority", Some("Nice to have. Stuff that probably won't be implemented."), "pri=low", Alive),
        TagGroup(10, "Status", None, NotEnumLike, Alive),
        ApplicableTag(11, "WIP", None, "wip", Alive),
        ApplicableTag(12, "Deferred", None, "defer", Alive),
        TagGroup(20, "Version", None, NotEnumLike, Alive),
        ApplicableTag(21, "v1.x", None, "v1.x", Alive),
        ApplicableTag(22, "v1.0", None, "v1.0", Alive),
        ApplicableTag(23, "v1.1", None, "v1.1", Alive),
        ApplicableTag(24, "v1.2", None, "v1.2", Alive),
        ApplicableTag(25, "v2.x", None, "v2.x", Alive),
        ApplicableTag(26, "v3.x", None, "v3.x", Dead),
        TagGroup(27, "Released", None, NotEnumLike, Alive)),
      BiMultimap(Multimap.empty[Tag.Id, Set, Tag.Id]
        .addvs(1, Set(2, 3, 4))
        .addvs(10, Set(11, 12))
        .addvs(20, Set(27, 21, 25, 26))
        .addvs(21, Set(22, 23, 24))
        .addvs(27, Set(22, 23))
      )))

    new Project(customImplTypes, customReqTypes, tags)
  }

  var p = newProject

  val projectInit = ServerProtocol.routine(Routines.ProjectInit)(_ => p)

  // -------------------------------------------------------------------------------------------------------------------
  object reqqq {

    def upd(id: CustomReqType.Id, f: CustomReqType => CustomReqType) =
      mod(_.map(c => if (c.id == id) f(c) else c))

    def modR(f: List[CustomReqType] => List[CustomReqType]): Option[Rev] = {
      val c = p.customReqTypes
      val a = c.data
      val b = f(a)
      if (a == b)
        None
      else {
        val rev = c.rev.succ
        p = p.copy(customReqTypes = DataSet[CustomReqType](rev, b))
        Some(rev)
      }
    }

    def δ(p: Project) = p.customReqTypes.data.map(c => c.id -> c).toMap

    def mod(f: List[CustomReqType] => List[CustomReqType]): RemoteDelta = {
      val p1 = p
      Thread.sleep(new java.util.Random().nextInt(120)+100)
      modR(f).map(rev => {
        val m1 = δ(p1)
        val m2 = δ(p)
        val delIds = m1.keySet -- m2.keySet
        val updates = m2.toStream.filter{ case (k,v) => !m1.contains(k) || m1(k) != v }.map(_._2).toList
        RemoteDeltaG(Partition.CustomReqTypes, rev, rev)(delIds, updates)
      }).toList
    }

    val crud =
      ServerProtocol.routine(Routines.CustomReqTypeCrud)({
        case CrudAction.Create(v)    =>
          val (mnemonic, name, imp) = v
          val id = CustomReqType.Id(p.customReqTypes.data.map(_.id.value).max + 1)
          val n = CustomReqType(id, mnemonic, Set.empty, name, imp, Alive)
          mod(n :: _)

        case CrudAction.Update(id, v) =>
          val (mnemonic, name, imp) = v
          upd(id, o => CustomReqType(id, mnemonic, (o.oldMnemonics + o.mnemonic) - mnemonic, name, imp, Alive))

        case CrudAction.Delete(id, HardDel) => mod(_.filterNot(_.id == id))
        case CrudAction.Delete(id, SoftDel) => upd(id, _.copy(alive = Dead))
        case CrudAction.Delete(id, Restore) => upd(id, _.copy(alive = Alive))
      })

    val imptoggle =
      ServerProtocol.routine(Routines.CustomReqTypeImplicationMod){
        case (id, imp2) => upd(id, _.copy(imp = imp2))
      }
  }

  // -------------------------------------------------------------------------------------------------------------------
  // TODO Another copy/paste/search/replace
  val incmpCrud = {

    def upd(id: CustomIncmpType.Id, f: CustomIncmpType => CustomIncmpType) =
      mod(_.map(c => if (c.id == id) f(c) else c))

    def modR(f: List[CustomIncmpType] => List[CustomIncmpType]): Option[Rev] = {
      val c = p.customIncmpTypes
      val a = c.data
      val b = f(a)
      if (a == b)
        None
      else {
        val rev = c.rev.succ
        p = p.copy(customIncmpTypes = DataSet[CustomIncmpType](rev, b))
        Some(rev)
      }
    }

    def δ(p: Project) = p.customIncmpTypes.data.map(c => c.id -> c).toMap

    def mod(f: List[CustomIncmpType] => List[CustomIncmpType]): RemoteDelta = {
      val p1 = p
      Thread.sleep(new java.util.Random().nextInt(120)+100)
      modR(f).map(rev => {
        val m1 = δ(p1)
        val m2 = δ(p)
        val delIds = m1.keySet -- m2.keySet
        val updates = m2.toStream.filter{ case (k,v) => !m1.contains(k) || m1(k) != v }.map(_._2).toList
        RemoteDeltaG(Partition.CustomIncmpTypes, rev, rev)(delIds, updates)
      }).toList
    }

    ServerProtocol.routine(Routines.CustomIncmpTypeCrud)({
      case CrudAction.Create(v)    =>
        val (key, desc) = v
        val id = CustomIncmpType.Id(p.customIncmpTypes.data.map(_.id.value).max + 1)
        val n = CustomIncmpType(id, key, desc, Alive)
        mod(n :: _)

      case CrudAction.Update(id, v) =>
        val (key, desc) = v
        upd(id, o => CustomIncmpType(id, key, desc, Alive))

      case CrudAction.Delete(id, HardDel) => mod(_.filterNot(_.id == id))
      case CrudAction.Delete(id, SoftDel) => upd(id, _.copy(alive = Dead))
      case CrudAction.Delete(id, Restore) => upd(id, _.copy(alive = Alive))
    })
  }

  // -------------------------------------------------------------------------------------------------------------------
  val tagCrud = {
    import Tag.Id
    import TagProtocol._

    def modR(f: TagTree => TagTree): Option[Rev] = {
      val rd = p.tags
      val a = rd.data
      val b = f(a)
      if (a == b)
        None
      else {
        val rev = rd.rev.succ
        p = p.copy(tags = RevAnd(rev, b))
        Some(rev)
      }
    }

    def δ(p: Project) = {
      val tt = p.tags.data
      tt.tags.mapValues(t => PovTag(t, PovRelations(tt.parentToChild(t.id), tt.childToParent(t.id))))
    }

    // TODO Another copy/paste/search/replace
    def mod(f: TagTree => TagTree): RemoteDelta = {
      val p1 = p
      Thread.sleep(new java.util.Random().nextInt(120)+100)
      modR(f).map(rev => {
        val m1 = δ(p1)
        val m2 = δ(p)
        val delIds = m1.keySet -- m2.keySet
        val updates = m2.toStream.filter{ case (k,v) => !m1.contains(k) || m1(k) != v }.map(_._2).toList
        RemoteDeltaG(Partition.Tags, rev, rev)(delIds, updates)
      }).toList
    }

    def upd(id: Id, f: Tag => Tag): RemoteDelta =
      mod(tt => tt.tags.get(id).fold(tt)(t =>
        tt.copy(tags = tt.tags + (id -> f(t)))))

    def put(i: Id, v: (Values, PovRelations)): RemoteDelta = {
      val (t, r) = v map1 build(i)
      mod(tt => TagTree(tt.tags + (i -> t), tt.structure.addas(r.parents, i).addbs(i, r.children)))
    }

    def nextId = Id(p.tags.data.tags.keySet.map(_.value).max + 1)

    def build(i: Id): Values => Tag = {
      case TagGroupValues(n, d, e)      => TagGroup(i, n, d, e, Alive)
      case ApplicableTagValues(n, d, k) => ApplicableTag(i, n, d, k, Alive)
    }

    ServerProtocol.routine(Routines.TagCrud)({
      case CrudAction.Create(v)           => put(nextId, v)
      case CrudAction.Update(i, v)        => put(i, v)
      case CrudAction.Delete(id, HardDel) => mod(tt => TagTree(tt.tags - id, tt.structure delab id))
      case CrudAction.Delete(id, SoftDel) => upd(id, Tag._alive setF Dead)
      case CrudAction.Delete(id, Restore) => upd(id, Tag._alive setF Alive)
    })
  }

  // -------------------------------------------------------------------------------------------------------------------
  def render = {
    val pg = Routines.ForCfgReqType(projectInit, incmpCrud, reqqq.crud, reqqq.imptoggle, tagCrud)
    val js = ServerProtocol.invokeClientHtml(JsEntryPoint.reactExamples)(pg)
    "*" #> js
  }
}
