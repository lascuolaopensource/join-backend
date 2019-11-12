package models.forms

import play.api.data.Form
import play.api.data.Forms.{mapping, _}
import play.api.data.validation.Constraints.{emailAddress, maxLength, nonEmpty}


object SignInForm {

  val form = Form(
    mapping(
      "email" -> text.verifying(nonEmpty, maxLength(255), emailAddress),
      "password" -> nonEmptyText,
      "rememberMe" -> boolean
    )(Data.apply)(Data.unapply)
  )

  case class Data(
    email: String,
    password: String,
    rememberMe: Boolean)

}
