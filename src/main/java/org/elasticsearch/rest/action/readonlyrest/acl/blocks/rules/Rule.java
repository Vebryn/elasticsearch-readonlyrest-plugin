package org.elasticsearch.rest.action.readonlyrest.acl.blocks.rules;

import com.google.common.base.CaseFormat;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.readonlyrest.acl.blocks.Block;

/**
 * Created by sscarduzio on 13/02/2016.
 */
abstract public class Rule {
  private Block.Policy policy = null;
  final public String KEY;

  protected RuleExitResult MATCH;
  protected RuleExitResult NO_MATCH;

  public Rule(Settings s) {
    KEY = CaseFormat.LOWER_CAMEL.to(
        CaseFormat.LOWER_UNDERSCORE,
        getClass().getSimpleName().replace("Rule", "")
    );
    MATCH = new RuleExitResult(true, this);
    NO_MATCH = new RuleExitResult(false, this);
  }

  public abstract RuleExitResult match(RestRequest request, RestChannel channel);

  public Block.Policy getPolicy() {
    return policy;
  }

}
