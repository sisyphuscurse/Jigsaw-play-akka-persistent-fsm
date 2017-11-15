package com.yiguan.order.service.receiption

import com.yiguan.order.service.core.request.OrderItem
import play.api.libs.json.Json

object OrderReceptionistCommand {
  case class RequestOrder(uid: Integer, items: List[OrderItem])
  case class NotifyOrderCancelled(orderId: String)

  case class NotifyOrderPaid(orderId: String, paymentId: String, paymentTime: String)
  object NotifyOrderPaid {
    implicit val formats = Json.format[NotifyOrderPaid]
  }

  case class NotifyOrderInDelivery(orderId: String, deliverId: String, deliverTime: String)
  object NotifyOrderInDelivery {
    implicit val formats = Json.format[NotifyOrderInDelivery]
  }

  case class NotifyOrderReceived(orderId: String, receivedTime: String)
  object NotifyOrderReceived {
    implicit val formats = Json.format[NotifyOrderReceived]
  }

  case class NotifyOrderConfirmed(orderId: String, confirmedTime: String)
  object NotifyOrderConfirmed {
    implicit val formats = Json.format[NotifyOrderConfirmed]
  }

  case class GetOrder(orderId: String)
}