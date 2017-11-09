package com.yiguan.order.service.core.request

import play.api.libs.json.Json

/**
  * Created by xupanpan on 2017/11/2.
  */
case class OrderItem(pid: Int, name: String, price: BigDecimal, amount: Int)
object OrderItem {
  implicit val formats = Json.format[OrderItem]
}

case class CreateOrderRequest(uid: Int, items: List[OrderItem])

object CreateOrderRequest {
  implicit val formats = Json.format[CreateOrderRequest]
}


