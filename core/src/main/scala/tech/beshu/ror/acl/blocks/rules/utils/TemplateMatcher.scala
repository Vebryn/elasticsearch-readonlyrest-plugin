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
package tech.beshu.ror.acl.blocks.rules.utils

import tech.beshu.ror.acl.blocks.rules.utils.StringTNaturalTransformation.instances.stringIndexNameNT
import tech.beshu.ror.acl.domain.IndexName

object TemplateMatcher {

  def findTemplatesIndicesPatterns(templatesPatterns: Set[IndexName],
                                   allowedIndices: Set[IndexName]): Set[IndexName] = {
    val filteredPatterns = MatcherWithWildcardsScalaAdapter
      .create(templatesPatterns)
      .filter(allowedIndices)
    if (filteredPatterns.nonEmpty) {
      filteredPatterns
    } else {
      MatcherWithWildcardsScalaAdapter
        .create(allowedIndices)
        .filter(templatesPatterns)
    }
  }
}
