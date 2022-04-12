package pl.agh.it.server

import java.time.format.DateTimeFormatter

/*
  LocalDateTime formatter for conversions from string to timestamp and vice versa
*/
trait LDTFormatterConfiguration {
  def formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
}
