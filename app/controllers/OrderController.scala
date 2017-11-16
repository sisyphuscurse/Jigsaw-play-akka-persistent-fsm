package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.yiguan.order.service.core.request.CreateOrderRequest
import com.yiguan.order.service.core.response.ApiResponse
import com.yiguan.order.service.order.OrderDetail
import com.yiguan.order.service.receiption.OrderReceptionistCommand._
import com.yiguan.order.service.receiption.{OrderReceptionist, OrderReceptionistCommand}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AbstractController, ControllerComponents, Request, Result}

import scala.concurrent.Future
import scala.concurrent.duration._

class OrderController @Inject()(system: ActorSystem, cc: ControllerComponents) extends AbstractController(cc) {
  val logger = Logger(this.getClass);

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val timeout: Timeout = 60.seconds
  val receptionist = system.actorOf(OrderReceptionist.props("order-receptionist"))


  def createOrder() = Action.async(parse.json) { request: Request[JsValue] =>
    request.body.validate[CreateOrderRequest].map { req =>
      (receptionist ? OrderReceptionistCommand.RequestOrder(req.uid, req.items)).mapTo[ApiResponse[OrderDetail]]
        .map(toResponse)
        .recover(requestFailed)
    }.getOrElse(Future.successful(BadRequest))
  }

  def getOrder(orderId: String) = Action.async {
    (receptionist ? OrderReceptionistCommand.GetOrder(orderId)).mapTo[ApiResponse[OrderDetail]].map(toResponse)
  }

  def setPaid() = Action.async(parse.json) { request: Request[JsValue] =>
    request.body.validate[NotifyOrderPaid].map { req =>
      (receptionist ? req).mapTo[ApiResponse[OrderDetail]]
        .map(toResponse)
        .recover(requestFailed)
    }.getOrElse(Future.successful(BadRequest))
  }

  def deliver() = Action.async(parse.json) { request: Request[JsValue] =>
    request.body.validate[NotifyOrderInDelivery].map { req =>
      (receptionist ? req).mapTo[ApiResponse[OrderDetail]]
        .map(toResponse)
        .recover(requestFailed)
    }.getOrElse(Future.successful(BadRequest))
  }

  def receive() = Action.async(parse.json) { request: Request[JsValue] =>
    request.body.validate[NotifyOrderReceived].map { req =>
      (receptionist ? req).mapTo[ApiResponse[OrderDetail]]
        .map(toResponse)
        .recover(requestFailed)
    }.getOrElse(Future.successful(BadRequest))
  }

  def confirm() = Action.async(parse.json) { request: Request[JsValue] =>
    request.body.validate[NotifyOrderConfirmed].map { req =>
      (receptionist ? req).mapTo[ApiResponse[OrderDetail]]
        .map(toResponse)
        .recover(requestFailed)
    }.getOrElse(Future.successful(BadRequest))
  }

  def cancel(orderId: String) = Action.async {
    (receptionist ? OrderReceptionistCommand.NotifyOrderCancelled(orderId)).mapTo[ApiResponse[OrderDetail]]
      .map(toResponse)
      .recover(requestFailed)
  }

  private def toResponse(result: ApiResponse[OrderDetail]) = Ok(Json.toJson(result))

  private def requestFailed: PartialFunction[Throwable, Result] = {
    case e: Throwable =>
      logger.error("requestFailed", e)
      Ok(Json.toJson(ApiResponse[OrderDetail](ApiResponse.REQUEST_ACCEPTED_FAILED, "请求失败")))
  }
}
