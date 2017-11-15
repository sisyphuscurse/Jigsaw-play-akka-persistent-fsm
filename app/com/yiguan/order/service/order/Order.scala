package com.yiguan.order.service.order

import akka.actor.{ActorRef, Props}
import akka.persistence.fsm.PersistentFSM
import com.yiguan.order.service.core.response.ApiResponse
import com.yiguan.order.service.order.OrderCommand._
import play.api.Logger

import scala.reflect.ClassTag
import scala.concurrent.duration._

class Order(pid: String, requestOrder: RequestOrder, questioner: Option[ActorRef]) extends PersistentFSM[OrderState, OrderStateData, OrderDomainEvent] {
  override implicit def domainEventClassTag: ClassTag[OrderDomainEvent] = ClassTag(classOf[OrderDomainEvent])

  val logger = Logger(this.getClass);

  override def persistenceId: String = pid

  val orderCreatedData = OrderCreatedData(requestOrder)
  questioner.foreach(f => f ! ApiResponse.apply(orderCreatedData.orderDetail))

  startWith(Created, orderCreatedData, Some(Order.PAY_TIMEOUT seconds))

  when(Created) {
    case Event(NotifyOrderPaid(paymentId, paymentTime, callbackActor), _) =>
      goto(Paid) applying OrderPaid(paymentId, paymentTime) andThen(f => callbackActor ! ApiResponse.apply(f.orderDetail))

    case Event(NotifyOrderCancelled(callbackActor), _) =>
      goto(Cancelled) applying OrderCancelled andThen (data => answerOrderDetail(data, callbackActor))

    case Event(StateTimeout, _) =>
      goto(Cancelled) applying OrderStateTimeout
  }

  when(Paid) {
    case Event(NotifyOrderInDelivery(deliverId, deliverTime, callbackActor), _) =>
      goto(InDelivery) applying OrderInDelivery(deliverId, deliverTime) andThen(data => answerOrderDetail(data, callbackActor))
  }

  when(InDelivery) {
    case Event(NotifyOrderReceived(receivedTime, callbackActor), _) =>
      goto(Received) applying OrderReceived(receivedTime) andThen(data => answerOrderDetail(data, callbackActor))
  }

  when(Received, Order.WAIT_CONFIRM_TIMEOUT seconds) {
    case Event(NotifyOrderConfirmed(confirmedTime, callbackActor), _) =>
      goto(Confirmed) applying OrderConfirmed(confirmedTime) andThen(data => answerOrderDetail(data, callbackActor))
    case Event(StateTimeout, _) =>
      goto(Confirmed) applying OrderStateTimeout
  }

  when(Confirmed) {
    case Event(GetOrder(callbackActor), _) =>
      stay applying GetOrderReplied andThen(data => answerOrderDetail(data, callbackActor))
  }

  when(Cancelled) {
    case Event(GetOrder(callbackActor), _) =>
      stay applying GetOrderReplied andThen(data => answerOrderDetail(data, callbackActor))
  }

  def answerOrderDetail(currentStateData: OrderStateData, questioner: ActorRef): Unit = {
    questioner ! ApiResponse.apply(currentStateData.orderDetail)
  }

  def answerIllegalCommand(currentStateData: OrderStateData, questioner: ActorRef): Unit = {
    questioner ! ApiResponse(ApiResponse.ILLEGAL_REQUEST, "该状态不支持此操作")
  }

  override def applyEvent(domainEvent: OrderDomainEvent, currentData: OrderStateData): OrderStateData = {
    domainEvent match {
      case event: OrderPaid => currentData.orderPaidFired(event.paymentId, event.paymentTime)
      case event: OrderInDelivery => currentData.orderInDeliveryFired(event.deliverId, event.deliverTime)
      case event: OrderReceived => currentData.orderReceivedFired(event.receivedTime)
      case event: OrderConfirmed => currentData.orderConfirmedFired(event.confirmedTime)
      case OrderCancelled => currentData.orderCancelled()
      case OrderStateTimeout => currentData.stateTimeout()
      case _ => currentData
    }
  }

  whenUnhandled {
    case Event(GetOrder(callbackActor), _) =>
      stay applying GetOrderReplied andThen(data => answerOrderDetail(data, callbackActor))
    case Event(NotifyOrderPaid(_, _, callbackActor), data) =>
      stay applying InvalidCommandReplied andThen(data => answerIllegalCommand(data, callbackActor))
    case Event(NotifyOrderCancelled(callbackActor), data) =>
      stay applying GetOrderReplied andThen(data => answerIllegalCommand(data, callbackActor))
    case Event(NotifyOrderInDelivery(_, _, callbackActor), data) =>
      stay applying InvalidCommandReplied andThen(data => answerIllegalCommand(data, callbackActor))
    case Event(NotifyOrderReceived(_, callbackActor), data) =>
      stay applying InvalidCommandReplied andThen(data => answerIllegalCommand(data, callbackActor))
    case Event(NotifyOrderConfirmed(_, callbackActor), data) =>
      stay applying InvalidCommandReplied andThen(data => answerIllegalCommand(data, callbackActor))
    case Event(event, _) => {
      stay andThen(f => logger.error(s"unhandled event:${event}"))
    }
  }

  override def onRecoveryCompleted(): Unit = stateName match {
    case Created => setStateTimeout(Created, Some(Order.PAY_TIMEOUT seconds))
    case Received => setStateTimeout(Received, Some(Order.WAIT_CONFIRM_TIMEOUT seconds))
  }
}

object Order {
  val PAY_TIMEOUT = 30 * 60
  val WAIT_CONFIRM_TIMEOUT = 2 * 24 * 3600

  def props(pid: String, requestOrder: RequestOrder, questioner: Option[ActorRef]) = Props(new Order(pid, requestOrder, questioner))
}