package models

import play.api.libs.ws.WS
import play.api.libs.ws.WS.WSRequestHolder
import org.joda.time.DateTime
import scala.concurrent.Future

/**
 * Author: chris
 * Created: 11/1/13
 */
class Repository(owner: String, name: String) {

  def getForks: Future[List[Fork]] = {
    val holder : WSRequestHolder = WS.url(s"https://api.github.com/repos/$owner/$name/forks")
    holder.get()

    // TODO parse json
  }

}

case class Fork(owner: String, name: String, forks: Int, watchers: Int, openIssues: Int, createdAt: DateTime, lastUpdatedAt: DateTime)
    extends Repository(owner, name)
