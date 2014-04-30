package shipreq.taskman.server.business

import com.squareup.okhttp.OkHttpClient
import java.io.InputStream
import java.net.{HttpURLConnection, URL}
import java.nio.charset.Charset
import org.apache.http.entity._
import org.apache.http.HttpEntity
import org.apache.http.util.EntityUtils
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scalaz.{\/, -\/, \/-}
import scalaz.effect.IO
import shipreq.base.util.effect.IOE
import shipreq.base.util.{Error, ErrorOr}
import shipreq.base.util.ScalaExt.AnyExt
import ErrorOr.Implicits._

object Http {

  sealed abstract class Method(val value: String)
  case object Get extends Method("GET")
  case object Put extends Method("PUT")
  case object Post extends Method("POST")
  case object Delete extends Method("DELETE")

  final case class Endpoint(url: URL, method: Method)

  final case class Req(e: Endpoint, bodyJ: JValue) {
    val bodyS = compact(bodyJ)
    def bodyB = bodyS.getBytes(defaultCharset)
  }

  val defaultCharset = Charset.forName("UTF-8")

  val contentTypeJson = s"application/json;charset=${defaultCharset.name}"

  def parseIntoJson(str: String): ErrorOr[JValue] = ErrorOr.safe(parse(str))
  def parseIntoJsonI(str: String): IOE[JValue]    = IO(parseIntoJson(str))

  // ---------------------------------------------------------------------------
  // Request

  def sendRequest(httpClient: OkHttpClient)(req: Req): IOE[HttpURLConnection] = {
    val io = openConn(httpClient, req.e)
    if (req.bodyS.isEmpty)
      io
    else
      io >==>^ writeRequestBody(req.bodyB)
  }

  def sendRequestL(httpClient: OkHttpClient, log: Req => IO[Unit])(req: Req): IOE[HttpURLConnection] =
    sendRequest(httpClient)(req) <<^ log(req)

  def openConn(httpClient: OkHttpClient, e: Endpoint): IOE[HttpURLConnection] = IOE {
    val conn = httpClient.open(e.url)
    conn.setRequestProperty("Content-Type", contentTypeJson)
    conn.setRequestMethod(e.method.value)
    conn
  }

  def writeRequestBody(body: Array[Byte])(conn: HttpURLConnection): IOE[Unit] =
    IO(ErrorOr.withResource(conn.getOutputStream)(_.close)(_ write body))

  // ---------------------------------------------------------------------------
  // Response

  def recv(f: HttpURLConnection => InputStream): HttpURLConnection => IOE[String] = conn =>
    IO(ErrorOr.withResource(f(conn))(_.close){ in =>
      val entity: HttpEntity = new InputStreamEntity(in)
      val charset = Option(ContentType get entity).map(_.getCharset) getOrElse defaultCharset
      val bytes = EntityUtils.toByteArray(entity)
      new String(bytes, charset)
    })

  val recvResponseInput = recv(_.getInputStream)

  val recvResponseError = recv(_.getErrorStream)

  def getResponseCode(conn: HttpURLConnection): IOE[Int] =
    IOE(conn.getResponseCode)

  def recvResponse[R, E](ep: ErrParser[E], log: String => IO[Unit])(ok: JValue => ErrorOr[R], er: E => Option[R])
                        (conn: HttpURLConnection): IOE[R] =
    getResponseCode(conn) >==> (code =>
      if (code == HttpURLConnection.HTTP_OK)
        recvResponseInput(conn) <-<^ log >=> (parseIntoJson(_) >=> ok)
      else
        recvResponseError(conn) <-<^ log >==> handleErrorResponse(ep, er, genericHttpError(conn))
      )

  // ---------------------------------------------------------------------------
  // Error handling

  case class ErrParser[E](parse: JValue => ErrorOr[E], mkError: E => Error)

  def handleErrorResponse[R, E](ep: ErrParser[E], mkResult: E => Option[R], fallback: String => IO[Error])(resp: String): IOE[R] =
    parseIntoJson(resp) >==> parseErrorJson(ep, mkResult) match {
      case \/-(-\/(e)) => IO(ep.mkError(e).toErrorOr)
      case \/-(\/-(r)) => IOE.pure(r)
      case -\/(_)      => fallback(resp).map(_.toErrorOr)
    }

  def parseErrorJson[R, E](ep: ErrParser[E], mkResult: E => Option[R]): JValue => ErrorOr[E \/ R] =
    ep.parse(_) >-> tryMkResult(mkResult)

  def tryMkResult[R, E](mkResult: E => Option[R])(e: E): E \/ R =
    mkResult(e) match {
      case Some(r) => \/-(r)
      case None    => -\/(e)
    }

  def genericHttpError(c: HttpURLConnection)(resp: String): IO[Error] =
    IO(Error(s"Unexpected HTTP response: ${c.getResponseCode} ${c.getResponseMessage}. Response: $resp"))
}
