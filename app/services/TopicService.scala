package services

import javax.inject.Inject

import models.Topic
import models.daos.BazaarIdeaDAO

import scala.concurrent.Future


trait TopicService extends Service {

  def search(topic: Option[String]): Future[Seq[Topic]]

}


class TopicServiceImpl @Inject()(bazaarIdeaDAO: BazaarIdeaDAO) extends TopicService {

  override def search(topic: Option[String]): Future[Seq[Topic]] = bazaarIdeaDAO.searchTopics(topic)

}
