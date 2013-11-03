package models

import play.api.libs.ws.WS
import org.joda.time.{DateTime, Duration}
import scala.concurrent.Future
import play.api.libs.json._
import scala.Some
import play.api.libs.ws.WS.WSRequestHolder

/**
 * Author: chris
 * Created: 11/1/13
 */

case class RepoId(owner: String, name: String) {
  override def toString: String = s"$owner/$name"
}

case class Repository(id: RepoId,
                      forkCount: Int,
                      starCount: Int,
                      openIssueCount: Int,
                      createdAt: DateTime,
                      lastPushedAt: DateTime,
                      lastUpdatedAt: DateTime) {

  def daysAgoPretty(daysAgo: Long) =
    daysAgo match {
      case 0 => "Today"
      case 1 => "Yesterday"
      case d => d + " days ago"
    }
  def createdPretty: String = daysAgoPretty(new Duration(createdAt, DateTime.now).getStandardDays)
  def lastPushPretty: String = {
    if (lastPushedAt.isBefore(createdAt)) "Never"
    else daysAgoPretty(new Duration(lastPushedAt, DateTime.now).getStandardDays)
  }

}

case class Credentials(clientId: String, clientSecret: String)

object OAuth {
  val credentials: Option[Credentials] = {
    for {
      id <- sys.env.get("GITHUB_API_CLIENT_ID")
      secret <- sys.env.get("GITHUB_API_CLIENT_SECRET")
    } yield Credentials(id, secret)
  }
}

object Repository {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val repositoryWrites = new Writes[Repository] {
    def writes(r: Repository): JsValue = {
      Json.obj(
        "owner" -> r.id.owner,
        "name" -> r.id.name,
        "fork_count" -> r.forkCount,
        "star_count" -> r.starCount,
        "open_issue_count" -> r.openIssueCount,
        "created_at" -> r.createdPretty,
        "last_pushed_at" -> r.lastPushPretty
      )
    }
  }

  def fromJson(json: JsValue): Repository = {
    Repository(
      id = RepoId((json \ "owner" \ "login").as[String], (json \ "name").as[String]),
      forkCount = (json \ "forks").as[Int],
      starCount = (json \ "watchers").as[Int],
      openIssueCount = (json \ "open_issues").as[Int],
      createdAt = new DateTime((json \ "created_at").as[String]),
      lastPushedAt = new DateTime((json \ "pushed_at").as[String]),
      lastUpdatedAt = new DateTime((json \ "updated_at").as[String])
    )
  }

  def ws(url: String): WSRequestHolder = {
    // If we have OAuth credentials, pass them in URL so we can get an increased rate limit
    val holder = WS.url(url)
    OAuth.credentials.fold(holder){ c =>
      holder.withQueryString("client_id" -> c.clientId, "client_secret" -> c.clientSecret)
    }
  }

  def select(id: RepoId): Future[Option[Repository]] = {
    ws(s"https://api.github.com/repos/$id").get().map { response =>
      Some(fromJson(response.json))
    }.fallbackTo(Future.successful(None))
  }

  def getForks(id: RepoId): Future[Seq[Repository]] = {
    ws(s"https://api.github.com/repos/$id/forks").withQueryString("sort" -> "watchers").get().map { response =>
      response.json.as[Seq[JsObject]].map(fromJson)
    }.fallbackTo(Future.successful(List()))
  }
}
