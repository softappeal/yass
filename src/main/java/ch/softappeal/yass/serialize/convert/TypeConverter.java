package ch.softappeal.yass.serialize.convert;

import ch.softappeal.yass.util.Check;

public abstract class TypeConverter {


  public final Class<?> type;

  TypeConverter(final Class<?> type) {
    this.type = Check.notNull(type);
  }

  public abstract void accept(Visitor visitor);


  public interface Visitor {

    void visit(IntegerTypeConverter typeConverter);

    void visit(LongTypeConverter typeConverter);

    void visit(StringTypeConverter typeConverter);

    void visit(BinaryTypeConverter typeConverter);

  }


}
