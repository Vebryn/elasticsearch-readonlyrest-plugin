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
package tech.beshu.ror.unit.acl.blocks.rules

import monix.execution.Scheduler.Implicits.global
import org.scalamock.scalatest.MockFactory
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import tech.beshu.ror.acl.blocks.BlockContext
import tech.beshu.ror.acl.blocks.rules.BasicAuthenticationRule
import tech.beshu.ror.acl.blocks.rules.Rule.RuleResult
import tech.beshu.ror.acl.domain.LoggedUser.DirectlyLoggedUser
import tech.beshu.ror.acl.domain.User.Id
import tech.beshu.ror.acl.request.RequestContext
import tech.beshu.ror.utils.TestsUtils.{StringOps, basicAuthHeader}

trait BasicAuthenticationTestTemplate extends WordSpec with MockFactory {

  protected def ruleName: String
  protected def rule: BasicAuthenticationRule[_]

  s"An $ruleName" should {
    "match" when {
      "basic auth header contains configured in rule's settings value" in {
        val requestContext = mock[RequestContext]
        val blockContext = mock[BlockContext]
        val modifiedBlockContext = mock[BlockContext]
        (requestContext.id _).expects().returning(RequestContext.Id("1"))
        (requestContext.headers _).expects().returning(Set(basicAuthHeader("logstash:logstash"))).twice()
        (blockContext.withLoggedUser _).expects(DirectlyLoggedUser(Id("logstash".nonempty))).returning(modifiedBlockContext)
        rule.check(requestContext, blockContext).runSyncStep shouldBe Right(RuleResult.Fulfilled(modifiedBlockContext))
      }
    }

    "not match" when {
      "basic auth header contains not configured in rule's settings value" in {
        val requestContext = mock[RequestContext]
        val blockContext = mock[BlockContext]
        (requestContext.id _).expects().returning(RequestContext.Id("1"))
        (requestContext.headers _).expects().returning(Set(basicAuthHeader("logstash:nologstash"))).twice()
        rule.check(requestContext, blockContext).runSyncStep shouldBe Right(RuleResult.Rejected())
      }
      "basic auth header is absent" in {
        val requestContext = mock[RequestContext]
        val blockContext = mock[BlockContext]
        (requestContext.id _).expects().returning(RequestContext.Id("1"))
        (requestContext.headers _).expects().returning(Set.empty).twice()
        rule.check(requestContext, blockContext).runSyncStep shouldBe Right(RuleResult.Rejected())
      }
    }
  }

}