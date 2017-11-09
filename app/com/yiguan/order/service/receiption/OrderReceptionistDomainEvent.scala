package com.yiguan.order.service.receiption

import com.yiguan.order.service.core.request.OrderItem
import com.yiguan.order.service.order.OrderDetail

/**
  * Created by xupanpan on 2017/10/31.
  */
sealed trait OrderReceptionistDomainEvent

case class OrderAccepted(orderId: String, uid: Integer, items: List[OrderItem]) extends OrderReceptionistDomainEvent
case class OrderCreated(orderId: String, totalPrice: BigDecimal, orderTime: String, orderState: String) extends OrderReceptionistDomainEvent
case class OrderPaid(orderId: String, paymentId: String, paymentTime: String, orderState: String) extends OrderReceptionistDomainEvent
case class OrderInDelivery(orderId: String, deliverId: String, deliverTime: String, orderState: String) extends OrderReceptionistDomainEvent
case class OrderReceived(orderId: String, receivedTime: String, orderState: String) extends OrderReceptionistDomainEvent
case class OrderConfirmed(orderId: String, confirmTime: String, orderState: String) extends OrderReceptionistDomainEvent
case class OrderCancelled(orderId: String, cancelTime: String, orderState: String) extends OrderReceptionistDomainEvent