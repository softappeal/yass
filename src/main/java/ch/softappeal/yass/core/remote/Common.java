package ch.softappeal.yass.core.remote;

import ch.softappeal.yass.util.Check;

public abstract class Common {

  Common(final MethodMapper.Factory methodMapperFactory) {
    this.methodMapperFactory = Check.notNull(methodMapperFactory);
  }

  public final MethodMapper.Factory methodMapperFactory;

  final MethodMapper methodMapper(final Class<?> contract) {
    return methodMapperFactory.create(contract);
  }

}
