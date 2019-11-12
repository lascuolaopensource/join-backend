package models

import models.forms.FormUtils.emptyMap
import play.api.data.Forms._
import play.api.data.validation.Constraint
import play.api.data.{FormError, Mapping}


package object forms {

  val passwordValidation = text(minLength = 8, maxLength = 32)

  // this solves the problem with optional empty lists when used with the custom FormUtils.fromJson
  case class OptionalListMapping[T] private[forms]
    (wrapped: Mapping[List[T]], constraints: Seq[Constraint[Option[List[T]]]] = Nil)
    extends Mapping[Option[List[T]]]
  {
    override val key: String = wrapped.key
    override val mappings: Seq[Mapping[_]] = wrapped.mappings

    override def bind(data: Map[String, String]): Either[Seq[FormError], Option[List[T]]] = {
      if (data.contains(key))
        Right(Some(List.empty[T]))
      else if (data.keys.exists(_.startsWith(key + "[")))
        wrapped.bind(data).right.map(Some(_))
      else
        Right(None)
    }

    override def unbind(value: Option[List[T]]): Map[String, String] = value.map(wrapped.unbind).getOrElse(emptyMap)

    override def unbindAndValidate(value: Option[List[T]]): (Map[String, String], Seq[FormError]) = {
      val errors = collectErrors(value)
      value.map(wrapped.unbindAndValidate).map(r => r._1 -> (r._2 ++ errors)).getOrElse(emptyMap -> errors)
    }

    override def withPrefix(prefix: String): Mapping[Option[List[T]]] =
      copy(wrapped = wrapped.withPrefix(prefix))

    override def verifying(addConstraints: Constraint[Option[List[T]]]*): Mapping[Option[List[T]]] =
      copy(constraints = constraints ++ addConstraints)
  }

  def optionalList[T](mapping: Mapping[T]) = OptionalListMapping(list(mapping))

}
