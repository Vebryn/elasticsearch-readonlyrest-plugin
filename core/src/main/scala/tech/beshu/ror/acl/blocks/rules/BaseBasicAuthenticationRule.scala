/*
 *    This file is part of ReadonlyREST.
 *
 *    ReadonlyREST is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    ReadonlyREST is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with ReadonlyREST.  If not, see http://www.gnu.org/licenses/
 */
package tech.beshu.ror.acl.blocks.rules

import cats.implicits._
import monix.eval.Task
import org.apache.logging.log4j.scala.Logging
import tech.beshu.ror.acl.blocks.BlockContext
import tech.beshu.ror.acl.blocks.rules.BasicAuthenticationRule.Settings
import tech.beshu.ror.acl.blocks.rules.Rule.RuleResult.{Fulfilled, Rejected}
import tech.beshu.ror.acl.blocks.rules.Rule.{AuthenticationRule, RuleResult}
import tech.beshu.ror.acl.domain.Credentials
import tech.beshu.ror.acl.domain.LoggedUser.DirectlyLoggedUser
import tech.beshu.ror.acl.request.RequestContext
import tech.beshu.ror.acl.request.RequestContextOps._
import tech.beshu.ror.acl.show.logs._

abstract class BaseBasicAuthenticationRule
  extends AuthenticationRule
    with Logging {

  protected def authenticateUsing(credentials: Credentials): Task[Boolean]

  override def tryToAuthenticate(requestContext: RequestContext,
                                 blockContext: BlockContext): Task[RuleResult] =
    Task
      .unit
      .flatMap { _ =>
        requestContext.basicAuth.map(_.credentials) match {
          case Some(credentials) =>
            logger.debug(s"Attempting Login as: ${credentials.user.show} rc: ${requestContext.id.show}")
            authenticateUsing(credentials)
              .map {
                case true => Fulfilled(blockContext.withLoggedUser(DirectlyLoggedUser(credentials.user)))
                case false => Rejected()
              }
          case None =>
            logger.debug(s"No basic auth, rc: ${requestContext.id.show}")
            Task.now(Rejected())
        }
      }
}

abstract class BasicAuthenticationRule[CREDENTIALS](val settings: Settings[CREDENTIALS])
  extends BaseBasicAuthenticationRule {

  override protected def authenticateUsing(credentials: Credentials): Task[Boolean] =
    compare(settings.credentials, credentials)

  protected def compare(configuredCredentials: CREDENTIALS, credentials: Credentials): Task[Boolean]
}

object BasicAuthenticationRule {

  final case class Settings[CREDENTIALS](credentials: CREDENTIALS)

}