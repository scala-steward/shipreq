package shipreq.webapp.util

import net.liftweb.util.Props
import net.liftweb.common.{Empty, Box, Full, Failure}
import shipreq.base.util.ExternalValueReader.Retriever

object PropsRetrievers {
  private implicit def unbox[T](b: Box[T]): Either[Option[String], T] = b match {
    case Full(v)          => Right(v)
    case Empty            => Left(None)
    case Failure(e, _, _) => Left(Some(e))
  }
  implicit val retrieverS = Retriever[String] (Props.get    (_))
  implicit val retrieverI = Retriever[Int]    (Props.getInt (_))
  implicit val retrieverL = Retriever[Long]   (Props.getLong(_))
  implicit val retrieverB = Retriever[Boolean](Props.getBool(_))
}
