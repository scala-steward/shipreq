package shipreq.webapp.base

import shipreq.base.util.IMap

package object delta {

  type RemoteDelta = IMap[Partition, RemoteDeltaPR]

  object RemoteDelta {
    val empty: RemoteDelta = IMap.empty(_.partition)
  }
}
