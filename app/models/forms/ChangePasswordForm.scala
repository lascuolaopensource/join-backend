package models.forms

import play.api.data.Form
import play.api.data.Forms._


object ChangePasswordForm {

  val form = Form(
    mapping(
      "current" -> nonEmptyText,
      "password1" -> passwordValidation,
      "password2" -> nonEmptyText
    )(Data.apply)(Data.unapply)
      .verifying("changePassword.passwords.notEqual", _.passwordsMatch))

  case class Data(current: String, password1: String, password2: String) {
    val passwordsMatch = password1 == password2
  }

}
