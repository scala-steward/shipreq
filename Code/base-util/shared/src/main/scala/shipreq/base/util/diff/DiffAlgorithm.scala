package shipreq.base.util.diff

trait DiffAlgorithm {

  def diff[A, P](original    : DiffSource[A],
                 revised     : DiffSource[A],
                 patchFactory: PatchFactory[P])
                (implicit A  : DiffEqual[A]): P
}
