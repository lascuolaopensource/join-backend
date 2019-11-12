package models.forms


object FormUtils {

  import play.api.libs.json._

  @inline private[forms] def emptyMap = Map.empty[String, String]

  // same as play.api.data.FormUtils but leaves key present for empty arrays.
  // allows the OptionalListMapper to recognize an empty array and operate accordingly.
  def fromJson(prefix: String = "", js: JsValue): Map[String, String] = js match {
    case JsObject(fields) =>
      fields.map {
        case (key, value) => fromJson(Option(prefix).filterNot(_.isEmpty).map(_ + ".").getOrElse("") + key, value)
      }.foldLeft(emptyMap)(_ ++ _)
    case JsArray(values) =>
      if (values.isEmpty) Map(prefix -> "")
      else values.zipWithIndex.map { case (value, i) => fromJson(prefix + "[" + i + "]", value) }.foldLeft(emptyMap)(_ ++ _)
    case JsNull => emptyMap
    case JsUndefined() => emptyMap
    case JsBoolean(value) => Map(prefix -> value.toString)
    case JsNumber(value) => Map(prefix -> value.toString)
    case JsString(value) => Map(prefix -> value.toString)
  }

}
