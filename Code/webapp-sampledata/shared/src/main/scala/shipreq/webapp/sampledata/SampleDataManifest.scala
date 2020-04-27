package shipreq.webapp.sampledata

final case class SampleDataMeta(filename: String, projectConfigHash: Int, projectContentHash: Int)

trait SampleDataManifest[D] {
  protected def load(meta: SampleDataMeta): D

  lazy val  `1000`: D = load(SampleDataMeta("shipreq-events-1000.json",    -15975327, -1910444130))
  lazy val  `2000`: D = load(SampleDataMeta("shipreq-events-2000.json",    454447920, -1851467186))
  lazy val  `4000`: D = load(SampleDataMeta("shipreq-events-4000.json",    106265614, -1357390324))
  lazy val `10000`: D = load(SampleDataMeta("shipreq-events-10000.json", -1981610195, -1390028356))

  lazy val all: Vector[D] =
    Vector(
       `1000`,
       `2000`,
       `4000`,
      `10000`)

  val byName: String => D = {
    case  "1000" =>  `1000`
    case  "2000" =>  `2000`
    case  "4000" =>  `4000`
    case "10000" => `10000`
  }
}
