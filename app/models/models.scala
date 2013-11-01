package models

import play.api.libs.ws.WS
import org.joda.time.{DateTime, Duration}
import scala.concurrent.Future
import play.api.libs.json._
import scala.Some

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

  def age: String = new Duration(createdAt, DateTime.now).getStandardDays + " days"
  def sinceLastPush: String = new Duration(lastPushedAt, DateTime.now).getStandardDays + " days"

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
        "age" -> r.age,
        "since_last_push" -> r.sinceLastPush
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

  def select(id: RepoId): Future[Option[Repository]] = {
    WS.url(s"https://api.github.com/repos/$id").get().map { response =>
      Some(fromJson(response.json))
    }.fallbackTo(Future.successful(None))
  }

  def getForks(id: RepoId): Future[Seq[Repository]] = {
    WS.url(s"https://api.github.com/repos/$id/forks").get().map { response =>
      response.json.as[Seq[JsObject]].map(fromJson)
    }.fallbackTo(Future.successful(List()))
  }
}
