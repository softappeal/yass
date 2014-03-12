import yass = require('../../main/ts/yass');

export var GENERATED_BY_YASS_VERSION = 'null';

export class PriceType extends yass.Enum {
  static BID = new PriceType(0, 'BID');
  static ASK = new PriceType(1, 'ASK');
  static TYPE_DESC = yass.enumDesc(6, PriceType);
}

export class Price extends yass.Class {
  instrumentId: string;
  type: PriceType;
  value: number;
  static TYPE_DESC = yass.classDesc(7, Price,
    new yass.FieldDesc(1, 'instrumentId', yass.STRING),
    new yass.FieldDesc(2, 'type', PriceType.TYPE_DESC),
    new yass.FieldDesc(3, 'value', yass.INTEGER)
  );
}

export class Instrument extends yass.Class {
  id: string;
  name: string;
}

export module instrument.stock {
  export class Stock extends Instrument {
    paysDividend: boolean;
    static TYPE_DESC = yass.classDesc(8, Stock,
      new yass.FieldDesc(1, 'id', yass.STRING),
      new yass.FieldDesc(2, 'name', yass.STRING),
      new yass.FieldDesc(3, 'paysDividend', yass.BOOLEAN)
    );
  }
}

export module instrument {
  export class Bond extends Instrument {
    coupon: number;
    static TYPE_DESC = yass.classDesc(9, Bond,
      new yass.FieldDesc(1, 'coupon', yass.INTEGER),
      new yass.FieldDesc(2, 'id', yass.STRING),
      new yass.FieldDesc(3, 'name', yass.STRING)
    );
  }
}

export class UnknownInstrumentsException extends yass.Class {
  comment: any;
  instrumentIds: string[];
  static TYPE_DESC = yass.classDesc(10, UnknownInstrumentsException,
    new yass.FieldDesc(1, 'comment', null),
    new yass.FieldDesc(2, 'instrumentIds', yass.LIST)
  );
}

export interface PriceEngine {
  subscribe(param0: string[]): void;
}
export interface PriceEngine_PROXY {
  subscribe(param0: string[]): yass.Promise<void>;
}
export var PriceEngine_MAPPER = new yass.MethodMapper<PriceEngine>(
  new yass.MethodMapping('subscribe', 0, false)
);

export interface PriceListener {
  echo(param0: string): string;
  newPrices(param0: Price[]): void;
}
export interface PriceListener_PROXY {
  echo(param0: string): yass.Promise<string>;
  newPrices(param0: Price[]): void;
}
export var PriceListener_MAPPER = new yass.MethodMapper<PriceListener>(
  new yass.MethodMapping('echo', 0, false),
  new yass.MethodMapping('newPrices', 1, true)
);

export module instrument {
  export interface InstrumentService {
    getInstruments(): Instrument[];
    reload(param0: boolean, param1: number): void;
  }
  export interface InstrumentService_PROXY {
    getInstruments(): yass.Promise<Instrument[]>;
    reload(param0: boolean, param1: number): void;
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
