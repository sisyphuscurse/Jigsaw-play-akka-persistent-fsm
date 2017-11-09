package com.yiguan.order.service.core.response

import com.yiguan.order.service.order.OrderDetail
import play.api.libs.json._

/**
  * Created by xupanpan on 2017/11/1.
  */
case class ApiResponse(ec: String, em: String, dto: Option[OrderDetail] = None)

object ApiResponse {
  def apply(dto: OrderDetail): ApiResponse = ApiResponse(REQUEST_ACCEPTED, "", Some(dto))

  implicit val formats = Json.format[ApiResponse]

  val REQUEST_ACCEPTED = "0"
  val REQUEST_ACCEPTED_FAILED = "101"
  val REQUEST_DUPLICATED = "102"
  val ILLEGAL_REQUEST = "500"
}

case class AResponse[T](ec: String, em: String, dto: Option[T])

object AResponse {
    implicit def writes[T](implicit owrites: OWrites[T]): OWrites[AResponse[T]] = new OWrites[AResponse[T]] {

    override def writes(o: AResponse[T]): JsObject = Json.obj(
      "ec" -> o.ec,
      "em" -> o.em,
      "dto" -> owrites.writes(o.dto.get)
    )

    def o(o: AResponse[T]): JsObject = Json.obj("ec" -> o.ec, "em" -> o.em) ++
      o.dto.map(owrites.writes).getOrElse(Json.obj())
  }
}

