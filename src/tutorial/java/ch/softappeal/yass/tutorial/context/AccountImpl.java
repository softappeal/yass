package ch.softappeal.yass.tutorial.context;

import ch.softappeal.yass.util.ContextLocator;
import ch.softappeal.yass.util.ContextService;

public class AccountImpl extends ContextService<String> implements Account {

  /**
   * @param userLocator shows dependency injection
   */
  public AccountImpl(final ContextLocator<String> userLocator) {
    super(userLocator);
  }

  @Override public int getBalance() {
    return "Alice".equals(context()) ? 200 : 100;
  }

}
