package models.forms

import play.api.data.Form
import play.api.data.Forms.{mapping, text, _}
import play.api.data.validation.Constraints._


object SignUpForm {

  val form = Form(
    mapping(
      "firstName" -> nonEmptyText(maxLength = 32),
      "lastName" -> nonEmptyText(maxLength = 32),
      "email" -> text.verifying(nonEmpty, maxLength(255), emailAddress),
      "password" -> passwordValidation,
      "password2" -> passwordValidation,
      "title" -> optional(text(maxLength = 255)),
      "agreement" -> boolean
    )(Data.apply)(Data.unapply)
      .verifying("signUp.error.passwordMatch", data => data.password == data.password2)
      .verifying("signUp.error.agreement", _.agreement)
  )

  case class Data (
    firstName: String,
    lastName: String,
    email: String,
    password: String,
    password2: String,
    title: Option[String],
    agreement: Boolean)

}
