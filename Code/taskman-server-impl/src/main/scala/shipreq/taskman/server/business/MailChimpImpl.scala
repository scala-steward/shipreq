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
import org.json4s.JsonDSL._
import scalaz.{-\/, \/-}
import scalaz.effect.IO
import shipreq.base.util.effect.{IOE, IOExt}
import shipreq.base.util.ErrorOr
import shipreq.base.util.Error
import shipreq.base.util.ScalaExt.AnyExt
import shipreq.base.util.log.HasLogger
import ErrorOr.Implicits._
import MailChimp._
import MailChimp.API._
import MailChimpImpl._

object MailChimpImpl {

  trait Props {
    val dc: String
    val key: String
    val masterList: String
  }

  class Req(val url: URL, val body: JValue)

  case class ApiFailure(code: Int, name: String, msg: String)

  val defaultCharset = Charset.forName("UTF-8")
  val contentTypeJson = s"application/json;charset=${defaultCharset.name}"

  def sendRequest(httpClient: OkHttpClient)(req: Req): IOE[HttpURLConnection] = {
    val bodyS = compact(req.body)
    val body = bodyS.getBytes(defaultCharset)
    openConn(httpClient, req.url) >==>^ writeRequestBody(body)
  }

  def openConn(httpClient: OkHttpClient, url: URL): IOE[HttpURLConnection] = IOE {
    val conn = httpClient.open(url)
    conn.setRequestProperty("Content-Type", contentTypeJson)
    conn.setRequestMethod("POST")
    conn
  }

  def writeRequestBody(body: Array[Byte])(conn: HttpURLConnection): IOE[Unit] =
    IO(ErrorOr.withResource(conn.getOutputStream)(_.close)(_ write body))

  def recv(f: HttpURLConnection => InputStream): HttpURLConnection => IOE[String] = conn =>
    IO(ErrorOr.withResource(f(conn))(_.close){ in =>
      val entity: HttpEntity = new InputStreamEntity(in)
      val charset = Option(ContentType get entity).map(_.getCharset) getOrElse defaultCharset
      val bytes = EntityUtils.toByteArray(entity)
      new String(bytes, charset)
    })

  val recvResponseInput = recv(_.getInputStream)
  val recvResponseError = recv(_.getErrorStream)

  def recvResponse(conn: HttpURLConnection): IOE[String] =
    getResponseCode(conn) >==> (code =>
      if (code == HttpURLConnection.HTTP_OK)
        recvResponseInput(conn)
      else
        handleErrorResponse(conn).map(_.toErrorOr)
      )

  def getResponseCode(conn: HttpURLConnection): IOE[Int] =
    IOE(conn.getResponseCode)

  def handleErrorResponse(conn: HttpURLConnection): IO[Error] = {
    val parseApiFailureOrGeneric: String => IO[Error] = resp =>
      parseErrorResponse(resp) match {
        case \/-(f) => IO(Error(s"[${f.code}] ${f.name}: ${f.msg}"))
        case -\/(_) => genericHttpError(conn, resp)
      }
    recvResponseError(conn) ftoErrorM parseApiFailureOrGeneric
  }

  def genericHttpError(c: HttpURLConnection, errResp: String): IO[Error] =
    IO(Error(s"Unexpected HTTP response: ${c.getResponseCode} ${c.getResponseMessage}. Response: $errResp"))

  def parseErrorResponse(resp: String): ErrorOr[ApiFailure] =
    parseJson(resp) >==> parseErrorResponseJson

  def parseJson(str: String): ErrorOr[JValue] =
    ErrorOr.safe(parse(str))

  def extractResult[R](a: API[R]): JValue => ErrorOr[R] =
    j => ErrorOr.safe(a match {

      case GetListId(_) =>
        val JInt(total) = j \ "total"
        total.toInt match {
          case 0 => None
          case 1 =>
            val JString(id) = (j \ "data")(0) \ "id"
            Some(ListId(id))
        }

    })
  
  def parseErrorResponseJson(j: JValue): ErrorOr[ApiFailure] =
    ErrorOr.catchException (
      (j \ "status") match {
        case JString("error") =>
          val JInt(code)    = j \ "code"
          val JString(name) = j \ "name"
          val JString(msg)  = j \ "error"
          ErrorOr(ApiFailure(code.toInt, name, msg))
        case _ => ErrorOr error "Not an error."
      }
    )
}

final class MailChimpImpl(httpClient: OkHttpClient, props: Props) extends HasLogger {

  private val urlPrefix = s"https://${props.dc}.api.mailchimp.com/2.0"
  private val apikeyJson = render("apikey" -> props.key)

  private object urls {
    def url(path: String) = new URL(s"$urlPrefix/$path")
    val lists_list = url("lists/list.json")
  }

  private def req(url: URL, reqJson: JValue): Req =
    new Req(url, apikeyJson merge reqJson)

  def run[A](api: API[A]): IOE[A] =
    (buildRequest(api) |> sendRequest(httpClient)) >==> recvResponse >=> parseJson >=> extractResult(api)

  val buildRequest: API[_] => Req = {
    case GetListId(name) =>
      req(urls.lists_list, "filters" -> ("list_name" -> name) ~ ("exact" -> true))
  }
}
