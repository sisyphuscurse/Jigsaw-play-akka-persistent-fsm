package com.yiguan.order.service.receiption

import akka.actor.ActorRef
import com.yiguan.order.service.order.{Created, OrderDetail}

case class OrderReceptionistData(ordersAccepted: Map[String, Boolean],
                                 orderActorsInProgress: Map[String, ActorRef],
                                 ordersInProgress: Map[String, OrderDetail],
                                 ordersCompleted: Map[String, OrderDetail]) {

  def orderAccepted(event: OrderAccepted, orderActor: ActorRef) = {
    copy(
      ordersAccepted = ordersAccepted + (event.orderId -> false),
      ordersInProgress = ordersInProgress + (event.orderId -> OrderDetail(event.orderId, event.uid, event.items)),
      orderActorsInProgress = orderActorsInProgress + (event.orderId -> orderActor)
    )
  }

  def orderCreated(event: OrderCreated) = {
    copy(ordersInProgress = ordersInProgress + (event.orderId -> ordersInProgress.get(event.orderId).map(f =>
      f.copy(totalPrice = Some(event.totalPrice), orderTime = Some(event.orderTime), orderState = Some(event.orderState))).get))
  }
  def orderPaid(event: OrderPaid) = {
    copy(ordersInProgress = ordersInProgress + (event.orderId -> ordersInProgress.get(event.orderId).map(f =>
      f.copy(paymentId = Some(event.paymentId), paymentTime = Some(event.paymentTime), orderState = Some(event.orderState))).get))
  }

  def orderInDelivery(event: OrderInDelivery) = {
    copy(ordersInProgress = ordersInProgress + (event.orderId -> ordersInProgress.get(event.orderId).map(f =>
      f.copy(deliverId = Some(event.deliverId), deliverTime = Some(event.deliverTime), orderState = Some(event.orderState))).get))
  }

  def orderReceived(event: OrderReceived) = {
    copy(ordersInProgress = ordersInProgress + (event.orderId -> ordersInProgress.get(event.orderId).map(f =>
      f.copy(receivedTime = Some(event.receivedTime), orderState = Some(event.orderState))).get))
  }

  def orderComfirmed(event: OrderConfirmed) = {
    copy(
      ordersAccepted = ordersAccepted + (event.orderId -> true),
      orderActorsInProgress = orderActorsInProgress - event.orderId,
      ordersCompleted = ordersCompleted + (event.orderId -> ordersInProgress.get(event.orderId).map(f =>
        f.copy(confirmedTime = Some(event.confirmTime), orderState = Some(event.orderState))).get),
      ordersInProgress = ordersInProgress - event.orderId
    )
  }

  def orderCancelled(event: OrderCancelled) = {
    copy(
      ordersAccepted = ordersAccepted + (event.orderId -> true),
      orderActorsInProgress = orderActorsInProgress - event.orderId,
      ordersCompleted = ordersCompleted + (event.orderId -> ordersInProgress.get(event.orderId).map(f =>
        f.copy(cancelTime = Some(event.cancelTime), orderState = Some(event.orderState))).get),
      ordersInProgress = ordersInProgress - event.orderId
    )
  }

  def getAcceptedOrderState(orderId: String) = ordersAccepted.get(orderId)

  def isOrderAccepted(orderId: String) = ordersAccepted.contains(orderId)

  def getOrderActorInProgress(orderId: String): Option[ActorRef] = orderActorsInProgress.get(orderId)

  def isOrderCompleted(orderId: String): Boolean = ordersAccepted.get(orderId).getOrElse(false)

  def getCompletedOrderDetail(orderId: String): Option[OrderDetail] = ordersCompleted.get(orderId)

  def getInProgressOrderDetail(orderId: String): Option[OrderDetail] = ordersInProgress.get(orderId)
}

object OrderReceptionistData {
  def empty() = OrderReceptionistData(Map[String, Boolean](), Map[String, ActorRef](), Map[String, OrderDetail](), Map[String, OrderDetail]())
}

case class OrderData(orderId: String, totalPrice: Double, orderState: String, completed: Boolean)
