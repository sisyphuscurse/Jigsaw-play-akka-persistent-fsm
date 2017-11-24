package controllers

import javax.inject._

import play.api._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, ws: WSClient) extends AbstractController(cc) {
  val logger = Logger(this.getClass);
  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def index1() = Action.async { implicit request: Request[AnyContent] =>
//    val yahoo = ws.url("http://www.baidu.com").get()
//    val google = ws.url("http://www.sina.com").get()
//    val bing = ws.url("http://www.163.com").get()
//    val all = Future.sequence(List(yahoo, google, bing))
//    all.map(r => Ok(Json.toJson(r.map(f => f.status))))

    Future.sequence("http://www.baidu.com" :: "http://www.sina.com" :: "http://www.163.com"
      :: Nil map(ws.url) map(_.get)).map(r => Ok(Json.toJson(r.map(f => f.status))))
//    val f = for {
//      yahoo <- ws.url("http://www.baidu.com").get()
//      google <- ws.url("http://www.sina.com").get()
//      bing <- ws.url("http://www.163.com").get()
//    } yield (yahoo, google, bing)

  }
}

