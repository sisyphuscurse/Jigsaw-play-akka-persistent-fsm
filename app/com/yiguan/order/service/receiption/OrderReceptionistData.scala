package com.yiguan.order.service.receiption

import akka.actor.ActorRef
import com.yiguan.order.service.order.{Created, OrderDetail}

case class OrderReceptionistData(ordersAccepted: Map[String, OrderDetail]) {

  def orderAccepted(event: OrderAccepted, orderActor: ActorRef) = {
    copy(
      ordersAccepted = ordersAccepted + (event.orderId -> OrderDetail(event.orderId, event.uid, event.items))
    )
  }

  def isOrderAccepted(orderId: String) = ordersAccepted.contains(orderId)
}

object OrderReceptionistData {
  def empty() = OrderReceptionistData(Map[String, OrderDetail]())
}