package com.yiguan.order.service.receiption

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ActorRef, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.persistence.PersistentActor
import com.yiguan.order.service.core.response.ApiResponse
import com.yiguan.order.service.order.{Order, OrderCommand}
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
          orderAccepted(event, Some(sender()))
        }
      }
    case cmd: OrderReceptionistCommand.GetOrder =>
      processInvalidOrderId(cmd.orderId, sender())
      context.child(cmd.orderId).foreach(f => f ! OrderCommand.GetOrder(sender()))

    case cmd: OrderReceptionistCommand.NotifyOrderPaid =>
      processInvalidOrderId(cmd.orderId, sender())
      context.child(cmd.orderId).foreach(f => f ! OrderCommand.NotifyOrderPaid(cmd.paymentId, cmd.paymentTime, sender()))
    case cmd: OrderReceptionistCommand.NotifyOrderInDelivery =>
      processInvalidOrderId(cmd.orderId, sender())
      context.child(cmd.orderId).foreach(f => f ! OrderCommand.NotifyOrderInDelivery(cmd.deliverId, cmd.deliverTime, sender()))
    case cmd: OrderReceptionistCommand.NotifyOrderReceived =>
      processInvalidOrderId(cmd.orderId, sender())
      context.child(cmd.orderId).foreach(f => f ! OrderCommand.NotifyOrderReceived(cmd.receivedTime, sender()))
    case cmd: OrderReceptionistCommand.NotifyOrderConfirmed =>
      processInvalidOrderId(cmd.orderId, sender())
      context.child(cmd.orderId).foreach(f => f ! OrderCommand.NotifyOrderConfirmed(cmd.confirmedTime, sender()))
    case cmd: OrderReceptionistCommand.NotifyOrderCancelled =>
      processInvalidOrderId(cmd.orderId, sender())
      context.child(cmd.orderId).foreach(f => f ! OrderCommand.NotifyOrderCancelled(sender()))

      // other commands
    case Terminated(orderActor) => logger.debug(s"orderActor terminated. orderActor:${orderActor}")

  }

  private def processInvalidOrderId(orderId: String, senderActor: ActorRef) = {
    if (!orderReceptionistData.isOrderAccepted(orderId)) sender() ! ApiResponse(ApiResponse.REQUEST_ACCEPTED, "订单不存在")
  }

  private def generateOrderId(requestOrder: OrderReceptionistCommand.RequestOrder) = {
    val orderId = s"${System.currentTimeMillis()}-${requestOrder.uid}"
    logger.debug(s"generateOrderId:${orderId}")
    orderId
  }

  override def receiveRecover: Receive = {
    case event: OrderAccepted => orderAccepted(event)
  }

  def orderAccepted(event: OrderAccepted, questioner: Option[ActorRef] = None) = {
    val orderActor = createOrderActor(OrderCommand.RequestOrder(event.orderId, event.uid, event.items), questioner)
    context.watch(orderActor)
    orderReceptionistData = orderReceptionistData.orderAccepted(event, orderActor)
  }

  def createOrderActor(request: OrderCommand.RequestOrder, questioner: Option[ActorRef] = None) = {
    context.actorOf(Order.props(request.orderId, request, questioner), request.orderId)
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
