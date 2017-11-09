package com.yiguan.order.service.order

import akka.actor.ActorRef
import com.yiguan.order.service.core.request.OrderItem

object OrderCommand {
  case class RequestOrder(orderId: String, uid: Integer, items: List[OrderItem])
  case object GetOrder

  case class NotifyOrderPaid(orderId: String, paymentId: String, paymentTime: String, callbackActor: ActorRef)
  case class NotifyOrderInDelivery(orderId: String, deliverId: String, deliverTime: String, callbackActor: ActorRef)
  case class NotifyOrderReceived(orderId: String, receivedTime: String, callbackActor: ActorRef)
  case class NotifyOrderConfirmed(orderId: String, confirmedTime: String, callbackActor: ActorRef)
  case class NotifyOrderCancelled(orderId: String, callbackActor: ActorRef)
}
