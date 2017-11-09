package com.yiguan.order.service.order

sealed trait OrderDomainEvent

case object OrderCreated extends OrderDomainEvent
case class OrderPaid(paymentId: String, paymentTime: String) extends OrderDomainEvent
case class OrderInDelivery(deliverId: String, deliverTime: String) extends OrderDomainEvent
case class OrderReceived(receivedTime: String) extends OrderDomainEvent
case class OrderConfirmed(confirmedTime: String) extends OrderDomainEvent
case object OrderCancelled extends OrderDomainEvent
case object OrderStateTimeout extends OrderDomainEvent

case object GetOrderReplied extends OrderDomainEvent

