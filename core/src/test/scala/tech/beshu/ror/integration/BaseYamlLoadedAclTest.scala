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
package tech.beshu.ror.integration

import java.time.Clock

import tech.beshu.ror.acl.Acl
import tech.beshu.ror.acl.factory.{CoreSettings, RawRorConfigBasedCoreFactory}
import tech.beshu.ror.mocks.MockHttpClientsFactory
import monix.execution.Scheduler.Implicits.global
import tech.beshu.ror.configuration.RawRorConfig
import tech.beshu.ror.providers._
import tech.beshu.ror.utils.TestsUtils.BlockContextAssertion

trait BaseYamlLoadedAclTest extends BlockContextAssertion {

  protected def configYaml: String

  protected implicit def envVarsProvider: EnvVarsProvider = OsEnvVarsProvider

  private val factory = {
    implicit val clock: Clock = Clock.systemUTC()
    implicit val uuidProvider: UuidProvider = JavaUuidProvider
    implicit val propertiesProvider: PropertiesProvider = JvmPropertiesProvider
    new RawRorConfigBasedCoreFactory
  }

  lazy val acl: Acl = factory
    .createCoreFrom(
      RawRorConfig.fromString(configYaml).right.get,
      MockHttpClientsFactory
    )
    .map {
      case Left(err) => throw new IllegalStateException(s"Cannot create ACL: $err")
      case Right(CoreSettings(aclEngine, _, _)) => aclEngine
    }
    .runSyncUnsafe()
}
