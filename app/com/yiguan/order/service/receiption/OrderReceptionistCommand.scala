package com.yiguan.order.service.receiption

import akka.actor.ActorRef
import com.yiguan.order.service.core.request.OrderItem
import com.yiguan.order.service.order.OrderDetail
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

  // Order notify OrderReceptionist
  case class ReportOrderCreated(orderId: String, totalPrice: BigDecimal, orderTime: String, orderState: String)
  case class ReportOrderPaid(orderId: String, paymentId: String, paymentTime: String, orderState: String)
  case class ReportOrderInDelivery(orderId: String, deliverId: String, deliverTime: String, orderState: String)
  case class ReportOrderReceived(orderId: String, receivedTime: String, orderState: String)

  case class ReportOrderConfirmed(orderId: String, confirmTime: String, orderState: String)
  case class ReportOrderCancelled(orderId: String, cancelTime: String, orderState: String)
}