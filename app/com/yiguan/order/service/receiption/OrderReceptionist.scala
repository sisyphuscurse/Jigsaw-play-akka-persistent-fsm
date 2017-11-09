package com.yiguan.order.service.receiption

import akka.pattern.ask
import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ActorRef, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.persistence.PersistentActor
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import com.yiguan.order.service.core.response.ApiResponse
import com.yiguan.order.service.order.{Created, Order, OrderCommand, OrderDetail}
import play.api.Logger

class OrderReceptionist(pid: String) extends PersistentActor {
  val logger = Logger(this.getClass);

  override def persistenceId: String = pid

  var orderReceptionistData = OrderReceptionistData.empty()

  override def receiveCommand: Receive = {
    // commands from controller
    case cmd: OrderReceptionistCommand.RequestOrder =>
      persist(OrderAccepted(generateOrderId(cmd), cmd.uid, cmd.items)) {
        event => {
          val senderActor = sender()
          val orderActor = orderAccepted(event, Some(senderActor))
          implicit val timeout: Timeout = 10.seconds
          (orderActor ? OrderCommand.GetOrder).mapTo[OrderDetail].foreach(orderDetail => {
              senderActor ! ApiResponse.apply(orderDetail)
              self ! OrderReceptionistCommand.ReportOrderCreated(orderDetail.orderId, orderDetail.totalPrice.get,
                orderDetail.orderTime.get, orderDetail.orderState.get)
            }
          )
        }
      }
    case cmd: OrderReceptionistCommand.GetOrder =>
      val acceptedOrderState = orderReceptionistData.getAcceptedOrderState(cmd.orderId)
      if (acceptedOrderState.isEmpty) sender() ! ApiResponse(ApiResponse.REQUEST_ACCEPTED, "订单不存在")
      else if (acceptedOrderState.get) sender() ! ApiResponse.apply(orderReceptionistData.getCompletedOrderDetail(cmd.orderId).get)
      else sender() ! ApiResponse(ApiResponse.REQUEST_ACCEPTED, "", orderReceptionistData.getInProgressOrderDetail(cmd.orderId))

    case cmd: OrderReceptionistCommand.NotifyOrderPaid =>
      processInvalidOrderId(cmd.orderId, sender())
      orderReceptionistData.getOrderActorInProgress(cmd.orderId).foreach((f => f ! OrderCommand.NotifyOrderPaid(cmd.orderId, cmd.paymentId, cmd.paymentTime, sender())))
    case cmd: OrderReceptionistCommand.NotifyOrderInDelivery =>
      processInvalidOrderId(cmd.orderId, sender())
      orderReceptionistData.getOrderActorInProgress(cmd.orderId).foreach(f => f ! OrderCommand.NotifyOrderInDelivery(cmd.orderId, cmd.deliverId, cmd.deliverTime, sender()))
    case cmd: OrderReceptionistCommand.NotifyOrderReceived =>
      processInvalidOrderId(cmd.orderId, sender())
      orderReceptionistData.getOrderActorInProgress(cmd.orderId).foreach(f => f ! OrderCommand.NotifyOrderReceived(cmd.orderId, cmd.receivedTime, sender()))
    case cmd: OrderReceptionistCommand.NotifyOrderConfirmed =>
      processInvalidOrderId(cmd.orderId, sender())
      orderReceptionistData.getOrderActorInProgress(cmd.orderId).foreach(f => f ! OrderCommand.NotifyOrderConfirmed(cmd.orderId, cmd.confirmedTime, sender()))
    case cmd: OrderReceptionistCommand.NotifyOrderCancelled =>
      processInvalidOrderId(cmd.orderId, sender())
      orderReceptionistData.getOrderActorInProgress(cmd.orderId).foreach(f => f ! OrderCommand.NotifyOrderCancelled(cmd.orderId, sender()))

    // commands from Order Actor
    case cmd: OrderReceptionistCommand.ReportOrderCreated =>
      persist(OrderCreated(cmd.orderId, cmd.totalPrice, cmd.orderTime, cmd.orderState)) {
        event => orderReceptionistData = orderReceptionistData.orderCreated(event)
      }
    case cmd: OrderReceptionistCommand.ReportOrderPaid =>
      persist(OrderPaid(cmd.orderId, cmd.paymentId, cmd.paymentTime, cmd.orderState)) {
        event => orderReceptionistData = orderReceptionistData.orderPaid(event)
      }
    case cmd: OrderReceptionistCommand.ReportOrderInDelivery => {
      persist(OrderInDelivery(cmd.orderId, cmd.deliverId, cmd.deliverTime, cmd.orderState)) {
        event => orderReceptionistData = orderReceptionistData.orderInDelivery(event)
      }
    }
    case cmd: OrderReceptionistCommand.ReportOrderReceived => {
      persist(OrderReceived(cmd.orderId, cmd.receivedTime, cmd.orderState)) {
        event => orderReceptionistData = orderReceptionistData.orderReceived(event)
      }
    }
    case cmd: OrderReceptionistCommand.ReportOrderConfirmed => {
      persist(OrderConfirmed(cmd.orderId, cmd.confirmTime, cmd.orderState)) {
        event =>
          orderCompleted(cmd.orderId)
          orderReceptionistData = orderReceptionistData.orderComfirmed(event)
      }
    }
    case cmd: OrderReceptionistCommand.ReportOrderCancelled => {
      persist(OrderCancelled(cmd.orderId, cmd.cancelTime, cmd.orderId)) {
        event =>
          orderCompleted(cmd.orderId)
          orderReceptionistData = orderReceptionistData.orderCancelled(event)
      }
    }

      // other commands
    case Terminated(orderActor) => logger.debug(s"orderActor terminated. orderActor:${orderActor}")

  }

  private def processInvalidOrderId(orderId: String, senderActor: ActorRef) = {
    val orderActor = orderReceptionistData.getOrderActorInProgress(orderId)
    if (orderActor.isEmpty) senderActor ! ApiResponse(ApiResponse.REQUEST_ACCEPTED, "订单不存在")
  }

  private def generateOrderId(requestOrder: OrderReceptionistCommand.RequestOrder) = {
    val orderId = s"${System.currentTimeMillis()}-${requestOrder.uid}"
    logger.debug(s"generateOrderId:${orderId}")
    orderId
  }

//  private def validateOrderId(orderId: String) = {
//    val orderActor = orderReceptionistData.getOrderActor(orderId)
//    if (!orderActor.isDefined) sender() ! ApiResponse(ApiResponse.ILLEGAL_REQUEST, "非法请求")
//    orderActor
//  }

  override def receiveRecover: Receive = {
    case event: OrderAccepted => orderAccepted(event)
    case event: OrderCreated => orderReceptionistData = orderReceptionistData.orderCreated(event)
    case event: OrderPaid => orderReceptionistData = orderReceptionistData.orderPaid(event)
    case event: OrderInDelivery => orderReceptionistData = orderReceptionistData.orderInDelivery(event)
    case event: OrderReceived => orderReceptionistData = orderReceptionistData.orderReceived(event)
    case event: OrderConfirmed =>
      orderCompleted(event.orderId)
      orderReceptionistData = orderReceptionistData.orderComfirmed(event)
    case event: OrderCancelled =>
      orderCompleted(event.orderId)
      orderReceptionistData = orderReceptionistData.orderCancelled(event)
  }

  def orderAccepted(event: OrderAccepted, questioner: Option[ActorRef] = None) = {
    val orderActor = createOrderActor(OrderCommand.RequestOrder(event.orderId, event.uid, event.items), questioner)
    context.watch(orderActor)
    orderReceptionistData = orderReceptionistData.orderAccepted(event, orderActor)
    orderActor
  }

  def createOrderActor(request: OrderCommand.RequestOrder, questioner: Option[ActorRef] = None) = {
    context.actorOf(Order.props(request.orderId, request, questioner))
  }

  def orderCompleted(orderId: String) = {
    orderReceptionistData.getOrderActorInProgress(orderId).foreach(f => context.stop(f))
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 3) {
    case t: Throwable =>
      logger.error("OrderReceptionist caught exception", t)
      Restart
  }
}

object OrderReceptionist {
  def props(pid: String) = Props(new OrderReceptionist(pid))
}
