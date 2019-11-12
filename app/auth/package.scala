import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import models.User

package object auth {

  type DefaultEnv = CookieEnv

  trait CookieEnv extends Env {
    override type I = User
    override type A = CookieAuthenticator
  }

}
