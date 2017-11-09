package com.yiguan.order.service.order

import akka.actor.{ActorRef, Props, Terminated}
import akka.persistence.fsm.PersistentFSM
import com.yiguan.order.service.core.response.ApiResponse
import com.yiguan.order.service.order.OrderCommand._
import com.yiguan.order.service.receiption.OrderReceptionistCommand
import play.api.Logger

import scala.reflect.ClassTag
import scala.concurrent.duration._

class Order(pid: String, requestOrder: RequestOrder, questioner: Option[ActorRef]) extends PersistentFSM[OrderState, OrderStateData, OrderDomainEvent] {
  override implicit def domainEventClassTag: ClassTag[OrderDomainEvent] = ClassTag(classOf[OrderDomainEvent])

  val logger = Logger(this.getClass);

  override def persistenceId: String = pid

  startWith(Created, OrderCreatedData(requestOrder), Some(Order.PAY_TIMEOUT seconds))

  when(Created) {
    case Event(NotifyOrderPaid(_, paymentId, paymentTime, callbackActor), _) =>
      goto(Paid) applying OrderPaid(paymentId, paymentTime) andThen(f => callbackActor ! ApiResponse.apply(f.orderDetail))

    case Event(NotifyOrderCancelled(_, callbackActor), _) =>
      goto(Cancelled) applying OrderCancelled andThen (
        data => {
          reportOrderCancelled(data)
          answerQuestioner(data, callbackActor)
        })
    case Event(GetOrder, data) =>
      stay applying GetOrderReplied andThen(data => sender() ! data.orderDetail)
    case Event(StateTimeout, _) =>
      goto(Cancelled) applying OrderStateTimeout andThen reportOrderCancelled
  }

  when(Paid) {
    case Event(NotifyOrderInDelivery(orderId, deliverId, deliverTime, callbackActor), _) =>
      goto(InDelivery) applying OrderInDelivery(deliverId, deliverTime) andThen(
        data => {
          reportOrderInDelivery(data)
          answerQuestioner(data, callbackActor)
        })
  }

  when(InDelivery) {
    case Event(NotifyOrderReceived(orderId, receivedTime, callbackActor), _) =>
      goto(Received) applying OrderReceived(receivedTime) andThen(
        data => {
          reportOrderReceived(data)
          answerQuestioner(data, callbackActor)
        })
  }

  when(Received, Order.WAIT_CONFIRM_TIMEOUT seconds) {
    case Event(NotifyOrderConfirmed(orderId, confirmedTime, callbackActor), _) =>
      goto(Confirmed) applying OrderConfirmed(confirmedTime) andThen(
        data => {
          reportOrderCompleted(data)
          answerQuestioner(data, callbackActor)
        })
    case Event(StateTimeout, _) =>
      goto(Confirmed) applying OrderStateTimeout andThen reportOrderCompleted
  }

  when(Confirmed) {
    case Event(_, _) => stay
  }

  when(Cancelled) {
    case Event(_, _) => stay
  }

  def answerQuestioner(currentStateData: OrderStateData, questioner: ActorRef): Unit = {
    questioner ! ApiResponse.apply(currentStateData.orderDetail)
  }

  def reportOrderCancelled(currentStateData: OrderStateData): Unit = {
    context.parent ! OrderReceptionistCommand.ReportOrderCancelled(currentStateData.orderDetail.orderId,
      currentStateData.orderDetail.cancelTime.get, currentStateData.orderDetail.orderState.get)
  }

  def reportOrderInDelivery(currentStateData: OrderStateData): Unit = {
    context.parent ! OrderReceptionistCommand.ReportOrderInDelivery(currentStateData.orderDetail.orderId, currentStateData.orderDetail.deliverId.get,
      currentStateData.orderDetail.deliverTime.get, currentStateData.orderDetail.orderState.get)
  }

  def reportOrderReceived(currentStateData: OrderStateData): Unit = {
    context.parent ! OrderReceptionistCommand.ReportOrderReceived(currentStateData.orderDetail.orderId, currentStateData.orderDetail.receivedTime.get,
      currentStateData.orderDetail.orderState.get)
  }

  def reportOrderCompleted(currentStateData: OrderStateData): Unit = {
    context.parent ! OrderReceptionistCommand.ReportOrderConfirmed(currentStateData.orderDetail.orderId,
      currentStateData.orderDetail.confirmedTime.get, currentStateData.orderDetail.orderState.get)
  }

  override def applyEvent(domainEvent: OrderDomainEvent, currentData: OrderStateData): OrderStateData = {
    domainEvent match {
      case event: OrderPaid => currentData.orderPaidFired(event.paymentId, event.paymentTime)
      case event: OrderInDelivery => currentData.orderInDeliveryFired(event.deliverId, event.deliverTime)
      case event: OrderReceived => currentData.orderReceivedFired(event.receivedTime)
      case event: OrderConfirmed => currentData.orderConfirmedFired(event.confirmedTime)
      case OrderCancelled => currentData.orderCancelled()
      case OrderStateTimeout => currentData.stateTimeout()
      case GetOrderReplied => currentData
    }
  }

  whenUnhandled {
    case Event(NotifyOrderCancelled(orderId, callbackActor), _) => {
      stay andThen(f => callbackActor ! ApiResponse(ApiResponse.ILLEGAL_REQUEST, "无法取消订单"))
    }
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