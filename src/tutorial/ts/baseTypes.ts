import yass = require('../../main/ts/yass');

class DateTime_HANDLER implements yass.TypeHandler<DateTime> {
  read(reader: yass.Reader): DateTime {
    return new DateTime(yass.STRING_DESC.handler.read(reader));
  }
  write(value: DateTime, writer: yass.Writer): void {
    yass.STRING_DESC.handler.write(value.dt, writer);
  }
}
export class DateTime extends yass.Type {
  constructor(public dt: string) {
    super();
  }
  static TYPE_DESC = new yass.TypeDesc(100, new DateTime_HANDLER);
}

export module instrument.stock {
  class JsDouble_HANDLER implements yass.TypeHandler<JsDouble> {
    read(reader: yass.Reader): JsDouble {
      return new JsDouble(new DataView(reader.array.buffer).getFloat64(reader.needed(8)));
    }
    write(value: JsDouble, writer: yass.Writer): void {
      var position = writer.needed(8);
      new DataView(writer.array.buffer).setFloat64(position, value.d);
    }
  }
  export class JsDouble extends yass.Type {
    constructor(public d: number) {
      super();
    }
    static TYPE_DESC = new yass.TypeDesc(101, new JsDouble_HANDLER);
  }
}
