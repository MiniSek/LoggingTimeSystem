package pl.agh.it.database.config

import scala.concurrent.duration.Duration

trait BlockingTime {
  def getBlockingTime: Duration = Duration("2 second")
}
