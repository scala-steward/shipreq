package shipreq.taskman.server.business

import org.specs2.mutable.Specification
import MailChimpImpl._
import MailChimp.API._
import MailChimp._
import shipreq.base.util.ErrorOr
import shipreq.base.util.ErrorOr.Implicits._

class MailChimpTest extends Specification {

  def parseE[R](a: API[R], json: String): ErrorOr[R] = parseJson(json) >==> extractResult(a)
  def parse[R](a: API[R], json: String): R = ErrorOr.require_!(parseE(a, json))

  "Error JSON" in {
    parseErrorResponse(
      """{"status":"error","code":553,"name":"Invalid_PagingLimit","error":"Page Limit Number must be greater than or equal to 0"}"""
    ) ==== ErrorOr(ApiFailure(553, "Invalid_PagingLimit", "Page Limit Number must be greater than or equal to 0"))
  }

  "lists/list" >> {

    "ok" in {
      parse(GetListId(""),
        """{"total":1,"data":[{"id":"270dff4105","web_id":340229,"name":"Master","date_created":"2014-04-16 07:20:13","email_type_option":false,"use_awesomebar":true,"default_from_name":"Yoar Mum","default_from_email":"yoar.mum@gmail.com","default_subject":"","default_language":"en","list_rating":0,"subscribe_url_short":"http:\/\/eepurl.com\/SKedX","subscribe_url_long":"http:\/\/twitter.us8.list-manage.com\/subscribe?u=53543f1bb4e0a0dacc73d54e2&id=270dff4105","beamer_address":"us8-0b1dbef7ba-68b7f9f73e@inbound.mailchimp.com","visibility":"pub","stats":{"member_count":0,"unsubscribe_count":0,"cleaned_count":0,"member_count_since_send":0,"unsubscribe_count_since_send":0,"cleaned_count_since_send":0,"campaign_count":0,"grouping_count":0,"group_count":0,"merge_var_count":3,"avg_sub_rate":0,"avg_unsub_rate":0,"target_sub_rate":0,"open_rate":0,"click_rate":0,"date_last_campaign":null},"modules":[]}],"errors":[]}"""
      ) must beSome(ListId("270dff4105"))
    }

    "no match" in {
      parse(GetListId(""), """{"total":0,"data":[],"errors":[]}""") must beNone
    }
  }
}
