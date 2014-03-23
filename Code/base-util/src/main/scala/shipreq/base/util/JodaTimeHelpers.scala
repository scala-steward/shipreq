package shipreq.base.util.jodatime

import org.joda.time.Period

object JodaTimeHelpers {

  implicit class Units(val n: Int) extends AnyVal {

    def ms           = Period millis n
    def millis       = Period millis n
    def millisecond  = Period millis n
    def milliseconds = Period millis n

    def sec          = Period seconds n
    def second       = Period seconds n
    def seconds      = Period seconds n

    def min          = Period minutes n
    def minute       = Period minutes n
    def minutes      = Period minutes n

    def hr           = Period hours n
    def hour         = Period hours n
    def hours        = Period hours n

    def day          = Period days n
    def days         = Period days n

    def month        = Period months n
    def months       = Period months n

    def week         = Period weeks n
    def weeks        = Period weeks n

    def year         = Period years n
    def years        = Period years n
  }

}
