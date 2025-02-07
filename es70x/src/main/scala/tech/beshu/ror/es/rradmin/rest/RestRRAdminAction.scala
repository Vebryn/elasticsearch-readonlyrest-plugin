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
package tech.beshu.ror.es.rradmin.rest

import org.elasticsearch.client.node.NodeClient
import org.elasticsearch.common.inject.Inject
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.rest.BaseRestHandler.RestChannelConsumer
import org.elasticsearch.rest.action.RestToXContentListener
import org.elasticsearch.rest._
import tech.beshu.ror.Constants
import tech.beshu.ror.adminapi._
import tech.beshu.ror.es.rradmin.{RRAdminAction, RRAdminRequest, RRAdminResponse}

@Inject
class RestRRAdminAction(settings: Settings, controller: RestController)
  extends BaseRestHandler(settings) with RestHandler {

  register("POST", AdminRestApi.forceReloadRorPath.endpointString)
  register("GET", AdminRestApi.provideRorIndexConfigPath.endpointString)
  register("POST", AdminRestApi.updateIndexConfigurationPath.endpointString)
  register("GET", AdminRestApi.provideRorFileConfigPath.endpointString)
  register("GET", Constants.REST_METADATA_PATH)
  
  override val getName: String = "ror-admin-handler"

  override def prepareRequest(request: RestRequest, client: NodeClient): RestChannelConsumer = (channel: RestChannel) => {
    client.execute(new RRAdminAction, new RRAdminRequest(request), new RestToXContentListener[RRAdminResponse](channel))
  }
  
  private def register(method: String, path: String): Unit = {
    controller.registerHandler(RestRequest.Method.valueOf(method), path, this)
  }
}
