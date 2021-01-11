package shipreq.base.util.diff

trait DiffAlgorithm[E] {

  final def diff[I, P](original    : I,
                       revised     : I)
                      (patchFactory: PatchFactory[I, P])
                      (implicit ds : DiffSource.Auto[I, E]): P = {

    val ctx = PatchFactory.Ctx(
      src = original,
      tgt = revised,
    )

    val p = patchFactory.newBuilder(ctx)

    writeDiff(
      original = ds wrap original,
      revised  = ds wrap revised,
      patch    = p,
    )

    p.result()
  }

  def writeDiff(original  : DiffSource[E],
                revised   : DiffSource[E],
                patch     : PatchWriter): Unit

}
