import yass = require('../../main/ts/yass');

export var GENERATED_BY_YASS_VERSION = 'null';

export class PriceType extends yass.Enum {
  static BID = new PriceType(0, 'BID');
  static ASK = new PriceType(1, 'ASK');
  static TYPE_DESC = yass.enumDesc(6, PriceType);
}

export class Price extends yass.Type {
  instrumentId: string;
  type: PriceType;
  value: number;
  static TYPE_DESC = yass.classDesc(7, Price,
    new yass.FieldDesc(1, 'instrumentId', yass.STRING_HANDLER),
    new yass.FieldDesc(2, 'type', PriceType.TYPE_DESC.handler),
    new yass.FieldDesc(3, 'value', yass.INTEGER_HANDLER)
  );
}

export class Instrument extends yass.Type {
  id: string;
  name: string;
}

export module instrument.stock {
  export class Stock extends Instrument {
    paysDividend: boolean;
    static TYPE_DESC = yass.classDesc(8, Stock,
      new yass.FieldDesc(1, 'id', yass.STRING_HANDLER),
      new yass.FieldDesc(2, 'name', yass.STRING_HANDLER),
      new yass.FieldDesc(3, 'paysDividend', yass.BOOLEAN_HANDLER)
    );
  }
}

export module instrument {
  export class Bond extends Instrument {
    coupon: number;
    static TYPE_DESC = yass.classDesc(9, Bond,
      new yass.FieldDesc(1, 'coupon', yass.INTEGER_HANDLER),
      new yass.FieldDesc(2, 'id', yass.STRING_HANDLER),
      new yass.FieldDesc(3, 'name', yass.STRING_HANDLER)
    );
  }
}

export class UnknownInstrumentsException extends yass.Type {
  comment: any;
  instrumentIds: string[];
  static TYPE_DESC = yass.classDesc(10, UnknownInstrumentsException,
    new yass.FieldDesc(1, 'comment', null),
    new yass.FieldDesc(2, 'instrumentIds', yass.LIST_HANDLER)
  );
}

export interface PriceEngine {
  subscribe(instrumentIds: string[]): void;
}
export interface PriceEngine_PROXY {
  subscribe(instrumentIds: string[]): yass.Promise<void>;
}
export var PriceEngine_MAPPER = new yass.MethodMapper<PriceEngine>(
  new yass.MethodMapping('subscribe', 0, false)
);

export interface PriceListener {
  echo(message: string): string;
  newPrices(prices: Price[]): void;
}
export interface PriceListener_PROXY {
  echo(message: string): yass.Promise<string>;
  newPrices(prices: Price[]): void;
}
export var PriceListener_MAPPER = new yass.MethodMapper<PriceListener>(
  new yass.MethodMapping('echo', 0, false),
  new yass.MethodMapping('newPrices', 1, true)
);

export module instrument {
  export interface InstrumentService {
    getInstruments(): Instrument[];
    reload(testBoolean: boolean, testInt: number): void;
  }
  export interface InstrumentService_PROXY {
    getInstruments(): yass.Promise<Instrument[]>;
    reload(testBoolean: boolean, testInt: number): void;
  }
  export var InstrumentService_MAPPER = new yass.MethodMapper<InstrumentService>(
    new yass.MethodMapping('getInstruments', 0, false),
    new yass.MethodMapping('reload', 1, true)
  );
}

export module ClientServices {
  export var PriceListener = new yass.ContractId<PriceListener, PriceListener_PROXY>(0, PriceListener_MAPPER);
}

export module ServerServices {
  export var PriceEngine = new yass.ContractId<PriceEngine, PriceEngine_PROXY>(0, PriceEngine_MAPPER);
  export var InstrumentService = new yass.ContractId<instrument.InstrumentService, instrument.InstrumentService_PROXY>(1, instrument.InstrumentService_MAPPER);
}

export var SERIALIZER = new yass.JsFastSerializer(
  PriceType,
  Price,
  instrument.stock.Stock,
  instrument.Bond,
  UnknownInstrumentsException
);
