package com.yiguan.order.service.order

import java.text.SimpleDateFormat
import java.util.Calendar

import com.yiguan.order.service.core.request.OrderItem
import play.api.libs.json.Json

sealed trait OrderStateData {
  // data
  def orderDetail: OrderDetail

  // behavior
  def orderPaidFired(paymentId: String, paymentTime: String): OrderStateData
  def orderInDeliveryFired(deliverId: String, deliverTime: String): OrderStateData
  def orderReceivedFired(receivedTime: String): OrderStateData
  def orderConfirmedFired(confirmedTime: String): OrderStateData
  def stateTimeout(): OrderStateData
  def orderCancelled(): OrderStateData

  // helpers
  def notSupported = this

  def now = {
    val cal = Calendar.getInstance();
    val dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormatter.format(cal.getTime)
  }
}

case class OrderDetail(orderId: String, uid: Int, items: List[OrderItem], totalPrice: Option[BigDecimal] = None, orderState: Option[String] = None,
                       orderTime: Option[String] = None, paymentId: Option[String] = None, paymentTime: Option[String] = None,
                       deliverId: Option[String] = None, deliverTime: Option[String] = None, receivedTime: Option[String] = None,
                       confirmedTime: Option[String] = None, cancelTime: Option[String] = None)
object OrderDetail {
  implicit val formats = Json.format[OrderDetail]
}

abstract class AbstractOrderStateData() extends OrderStateData {
  override def orderPaidFired(paymentId: String, paymentTime: String): OrderStateData = notSupported

  override def orderInDeliveryFired(deliverId: String, deliverTime: String): OrderStateData = notSupported

  override def orderReceivedFired(receivedTime: String): OrderStateData = notSupported

  override def orderConfirmedFired(confirmedTime: String): OrderStateData = notSupported

  override def stateTimeout(): OrderStateData = notSupported

  override def orderCancelled(): OrderStateData = notSupported
}

case class OrderCreatedData(requestOrder: OrderCommand.RequestOrder) extends AbstractOrderStateData {
  override def orderDetail: OrderDetail = OrderDetail(requestOrder.orderId, requestOrder.uid, requestOrder.items, Some(totalPrice()), Some(Created.identifier), Some(now))

  override def orderPaidFired(paymentId: String, paymentTime: String): OrderStateData = OrderPaidData(orderDetail, paymentId, paymentTime)

  override def stateTimeout(): OrderStateData = OrderCancelledData(orderDetail, now)

  override def orderCancelled(): OrderStateData = OrderCancelledData(orderDetail, now)

  private def totalPrice() = requestOrder.items.foldLeft(BigDecimal(0))((total, item) => total + item.price * item.amount)
}

case class OrderPaidData(oldOrderDetail: OrderDetail, paymentId: String, paymentTime: String) extends AbstractOrderStateData {
  override def orderDetail: OrderDetail = oldOrderDetail.copy(orderState = Some(Paid.identifier), paymentId = Some(paymentId), paymentTime = Some(paymentTime))

  override def orderInDeliveryFired(deliverId: String, deliverTime: String): OrderStateData = OrderInDeliveryData(orderDetail, deliverId, deliverTime)
}

case class OrderInDeliveryData(oldOrderDetail: OrderDetail, deliverId: String, deliverTime: String) extends AbstractOrderStateData {
  override def orderDetail: OrderDetail = oldOrderDetail.copy(orderState = Some(InDelivery.identifier), deliverId = Some(deliverId), deliverTime = Some(deliverTime))

  override def orderReceivedFired(receivedTime: String): OrderStateData = OrderReceivedData(orderDetail, receivedTime)
}

case class OrderReceivedData(oldOrderDetail: OrderDetail, receivedTime: String) extends AbstractOrderStateData {
  override def orderDetail: OrderDetail = oldOrderDetail.copy(orderState = Some(Received.identifier), receivedTime = Some(receivedTime))

  override def orderConfirmedFired(confirmedTime: String): OrderStateData = OrderConfirmedData(orderDetail, confirmedTime)

  override def stateTimeout(): OrderStateData = OrderConfirmedData(orderDetail, now)
}

case class OrderConfirmedData(oldOrderDetail: OrderDetail, confirmedTime: String) extends AbstractOrderStateData {
  override def orderDetail: OrderDetail = oldOrderDetail.copy(orderState = Some(Confirmed.identifier), confirmedTime = Some(confirmedTime))
}

case class OrderCancelledData(oldOrderDetail: OrderDetail, cancelTime: String) extends AbstractOrderStateData {
  override def orderDetail: OrderDetail = oldOrderDetail.copy(orderState = Some(Cancelled.identifier), cancelTime = Some(cancelTime))
}