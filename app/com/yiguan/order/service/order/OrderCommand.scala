package com.yiguan.order.service.order

import akka.actor.ActorRef
import com.yiguan.order.service.core.request.OrderItem

object OrderCommand {
  case class RequestOrder(orderId: String, uid: Integer, items: List[OrderItem])
  case class GetOrder(callbackActor: ActorRef)

  case class NotifyOrderPaid(paymentId: String, paymentTime: String, callbackActor: ActorRef)
  case class NotifyOrderInDelivery(deliverId: String, deliverTime: String, callbackActor: ActorRef)
  case class NotifyOrderReceived(receivedTime: String, callbackActor: ActorRef)
  case class NotifyOrderConfirmed(confirmedTime: String, callbackActor: ActorRef)
  case class NotifyOrderCancelled(callbackActor: ActorRef)
}
