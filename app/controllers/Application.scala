package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json
import models.{RepoId, Repository}

import scala.concurrent.ExecutionContext.Implicits.global

object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def repo(owner: String, name: String) = Action.async {
    val repo = Repository.select(RepoId(owner, name))
    repo.map { opt =>
      opt.fold (NotFound("")) { r => Ok(Json.toJson(r)) }
    }
  }

  def forks(owner: String, name: String) = Action.async {
    val forks = Repository.getForks(RepoId(owner, name))
    forks.map { fs =>
      Ok(Json.toJson(fs))
    }
  }

}