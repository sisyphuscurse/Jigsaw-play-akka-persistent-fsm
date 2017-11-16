package com.yiguan.order.service.receiption

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.persistence.inmemory.extension.{InMemoryJournalStorage, InMemorySnapshotStorage, StorageExtension}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.yiguan.order.service.core.request.OrderItem
import com.yiguan.order.service.core.response.ApiResponse
import com.yiguan.order.service.order._
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.Some
import scala.concurrent.duration._

/**
  * Created by xupanpan on 2017/11/16.
  */
class OrderReceptionistTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  override protected def beforeEach(): Unit = {
    val tp = TestProbe()
    tp.send(StorageExtension(system).journalStorage, InMemoryJournalStorage.ClearJournal)
    tp.expectMsg(akka.actor.Status.Success(""))
    tp.send(StorageExtension(system).snapshotStorage, InMemorySnapshotStorage.ClearSnapshots)
    tp.expectMsg(akka.actor.Status.Success(""))
    super.beforeEach()
  }


  "OrderReceptionist actor" must {
    "send back order detail with Created order state" in {
      val receptionist = system.actorOf(OrderReceptionist.props("receptionist"))
      within(1000 millis) {
        val response = sendRequestOrder(1001, orderItems, receptionist)

        response.dto.isDefined should be(true)

        val orderDetail = response.dto.get
        orderDetail.orderId.isEmpty should be(false)
        orderDetail.orderState should be(Some(Created.identifier))
        orderDetail.totalPrice should be(Some(totalPrice))
      }
      receptionist ! PoisonPill
    }

  }

  "OrderReceptionist actor" must {
    "send back order detail with Paid order state" in {
      val receptionist = system.actorOf(OrderReceptionist.props("receptionist"))
      within(1000 millis) {
        val response = sendRequestOrder(1001, orderItems, receptionist)

        val paymentId = "123456"
        val paymentTime = "2017-11-14 11:11:11"
        val orderId = response.dto.get.orderId
        val paidResponse = sendNotifyOrderPaid(orderId, paymentId, paymentTime, receptionist)

        paidResponse.dto.isDefined should be(true)
        val orderDetail = paidResponse.dto.get

        orderDetail.orderId should be(orderId)
        orderDetail.orderState should be(Some(Paid.identifier))
        orderDetail.paymentId should be(Some(paymentId))
        orderDetail.paymentTime should be(Some(paymentTime))
      }
      receptionist ! PoisonPill
    }
  }


  "OrderReceptionist actor" must {
    "send back order detail with InDelivery order state" in {
      val receptionist = system.actorOf(OrderReceptionist.props("receptionist"))
      within(1000 millis) {
        val response = sendRequestOrder(1001, orderItems, receptionist)

        val orderId = response.dto.get.orderId
        sendNotifyOrderPaid(orderId, "123456", "2017-11-14 11:11:11", receptionist)

        val deliveryId = "43456673"
        val deliveryTime = "2017-11-15 11:11:11"
        val inDeliveryResponse = sendNotifyOrderInDelivery(orderId, deliveryId, deliveryTime, receptionist)

        inDeliveryResponse.dto.isDefined should be(true)
        val orderDetail = inDeliveryResponse.dto.get
        orderDetail.orderId should be(orderId)
        orderDetail.orderState should be(Some(InDelivery.identifier))
        orderDetail.deliverId should be(Some(deliveryId))
        orderDetail.deliverTime should be(Some(deliveryTime))
      }
      receptionist ! PoisonPill
    }
  }

  "OrderReceptionist actor" must {
    "send back order detail with Received order state" in {
      val receptionist = system.actorOf(OrderReceptionist.props("receptionist"))
      within(1000 millis) {
        val response = sendRequestOrder(1001, orderItems, receptionist)

        val orderId = response.dto.get.orderId
        sendNotifyOrderPaid(orderId, "123456", "2017-11-14 11:11:11", receptionist)

        sendNotifyOrderInDelivery(orderId, "43456673", "2017-11-15 11:11:11", receptionist)

        val receivedTime = "2017-11-18 11:11:11"
        val receivedResponse = sendNotifyOrderReceived(orderId, receivedTime, receptionist)

        receivedResponse.dto.isDefined should be(true)
        val orderDetail = receivedResponse.dto.get
        orderDetail.orderId should be(orderId)
        orderDetail.orderState should be(Some(Received.identifier))
        orderDetail.receivedTime should be(Some(receivedTime))
      }
      receptionist ! PoisonPill
    }
  }

  "OrderReceptionist actor" must {
    "send back order detail with Confirmed order state" in {
      val receptionist = system.actorOf(OrderReceptionist.props("receptionist"))
      within(1000 millis) {
        val response = sendRequestOrder(1001, orderItems, receptionist)

        val orderId = response.dto.get.orderId
        sendNotifyOrderPaid(orderId, "123456", "2017-11-14 11:11:11", receptionist)

        sendNotifyOrderInDelivery(orderId, "43456673", "2017-11-15 11:11:11", receptionist)

        sendNotifyOrderReceived(orderId, "2017-11-18 11:11:11", receptionist)

        val confirmedTime = "2017-11-18 20:11:11"
        receptionist ! OrderReceptionistCommand.NotifyOrderConfirmed(orderId, confirmedTime)

        val confirmedResponse = expectMsgType[ApiResponse[OrderDetail]]
        confirmedResponse.dto.isDefined should be(true)
        val orderDetail = confirmedResponse.dto.get
        orderDetail.orderId should be(orderId)
        orderDetail.orderState should be(Some(Confirmed.identifier))
        orderDetail.confirmedTime should be(Some(confirmedTime))
      }
      receptionist ! PoisonPill
    }
  }

  "OrderReceptionist actor" must {
    "send back order detail with Cancelled order state" in {
      val receptionist = system.actorOf(OrderReceptionist.props("receptionist"))
      within(1000 millis) {
        val response = sendRequestOrder(1001, orderItems, receptionist)

        val orderId = response.dto.get.orderId
        receptionist ! OrderReceptionistCommand.NotifyOrderCancelled(orderId)
        val cancelledResponse = expectMsgType[ApiResponse[OrderDetail]]
        cancelledResponse.dto.isDefined should be(true)
        val orderDetail = cancelledResponse.dto.get
        orderDetail.orderId should be(orderId)
        orderDetail.orderState should be(Some(Cancelled.identifier))
      }
      receptionist ! PoisonPill
    }
  }


  "OrderReceptionist actor" must {
    "send back illegal request when doing cancel after paid" in {
      val receptionist = system.actorOf(OrderReceptionist.props("receptionist"))
      within(1000 millis) {
        val response = sendRequestOrder(1001, orderItems, receptionist)

        val orderId = response.dto.get.orderId
        sendNotifyOrderPaid(orderId, "123456", "2017-11-14 11:11:11", receptionist)

        receptionist ! OrderReceptionistCommand.NotifyOrderCancelled(orderId)
        expectMsgType[ApiResponse[OrderDetail]] should be(ApiResponse(ApiResponse.ILLEGAL_REQUEST, "该状态不支持此操作"))
      }
      receptionist ! PoisonPill
    }
  }

  val orderItems: List[OrderItem] = List[OrderItem](
    OrderItem(1, "牙刷", BigDecimal(10), 2),
    OrderItem(2, "牙膏", BigDecimal(35.2), 3),
    OrderItem(3, "毛巾", BigDecimal(25.5), 1)
  )

  val totalPrice = orderItems.foldLeft(BigDecimal(0))((total, orderItem) => total + orderItem.price * orderItem.amount)

  def sendRequestOrder(uid: Integer, orderItems: List[OrderItem], receptionist: ActorRef): ApiResponse[OrderDetail] = {
    receptionist ! OrderReceptionistCommand.RequestOrder(1001, orderItems)
    expectMsgType[ApiResponse[OrderDetail]]
  }

  def sendNotifyOrderPaid(orderId: String, paymentId: String, paymentTime: String, receptionist: ActorRef): ApiResponse[OrderDetail] = {
    receptionist ! OrderReceptionistCommand.NotifyOrderPaid(orderId, paymentId, paymentTime)
    expectMsgType[ApiResponse[OrderDetail]]
  }

  def sendNotifyOrderInDelivery(orderId: String, deliveryId: String, deliveryTime: String, receptionist: ActorRef): ApiResponse[OrderDetail] = {
    receptionist ! OrderReceptionistCommand.NotifyOrderInDelivery(orderId, deliveryId, deliveryTime)
    expectMsgType[ApiResponse[OrderDetail]]
  }

  def sendNotifyOrderReceived(orderId: String, receivedTime: String, receptionist: ActorRef): ApiResponse[OrderDetail] = {
    receptionist ! OrderReceptionistCommand.NotifyOrderReceived(orderId, receivedTime)
    expectMsgType[ApiResponse[OrderDetail]]
  }
}
