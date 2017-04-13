import * as yass from "../../yass";
import {Integer, IntegerHandler} from "../baseTypes-external";

// shows how to use contract internal base types
class ExpirationHandler implements yass.TypeHandler<Expiration> {
    read(reader: yass.Reader): Expiration {
        return new Expiration(
            reader.readZigZagInt(),
            reader.readZigZagInt(),
            reader.readZigZagInt()
        );
    }
    write(value: Expiration, writer: yass.Writer): void {
        writer.writeZigZagInt(value.year);
        writer.writeZigZagInt(value.month);
        writer.writeZigZagInt(value.day);
    }
    static readonly TYPE_DESC = new yass.TypeDesc(yass.FIRST_DESC_ID + 1, new ExpirationHandler());
}
export class Expiration {
    constructor(public readonly year: number, public readonly month: number, public readonly day: number) {
        // empty
    }
    static readonly TYPE_DESC = ExpirationHandler.TYPE_DESC;
}

export class PriceKind extends yass.Enum {
    static readonly BID = new PriceKind(0, 'BID');
    static readonly ASK = new PriceKind(1, 'ASK');
    static readonly VALUES = <PriceKind[]>yass.enumValues(PriceKind);
    static readonly TYPE_DESC = yass.enumDesc(9, PriceKind);
}

export class Price {
    instrumentId: Integer;
    kind: PriceKind;
    value: Integer;
    static readonly TYPE_DESC = yass.classDesc(10, Price,
        new yass.FieldDesc(1, 'instrumentId', IntegerHandler.TYPE_DESC),
        new yass.FieldDesc(2, 'kind', PriceKind.TYPE_DESC),
        new yass.FieldDesc(3, 'value', IntegerHandler.TYPE_DESC)
    );
}

export abstract class Instrument {
    id: Integer;
    name: string;
}

export namespace instrument.stock {
    export class Stock extends Instrument {
        paysDividend: boolean;
        static readonly TYPE_DESC = yass.classDesc(11, Stock,
            new yass.FieldDesc(1, 'paysDividend', yass.BOOLEAN_DESC),
            new yass.FieldDesc(2, 'id', IntegerHandler.TYPE_DESC),
            new yass.FieldDesc(3, 'name', yass.STRING_DESC)
        );
    }
}

export namespace instrument {
    export class Bond extends Instrument {
        coupon: number;
        expiration: Expiration;
        static readonly TYPE_DESC = yass.classDesc(12, Bond,
            new yass.FieldDesc(1, 'coupon', yass.NUMBER_DESC),
            new yass.FieldDesc(2, 'expiration', Expiration.TYPE_DESC),
            new yass.FieldDesc(3, 'id', IntegerHandler.TYPE_DESC),
            new yass.FieldDesc(4, 'name', yass.STRING_DESC)
        );
    }
}

export class SystemException {
    message: string;
    static readonly TYPE_DESC = yass.classDesc(13, SystemException,
        new yass.FieldDesc(1, 'message', yass.STRING_DESC)
    );
}

export abstract class ApplicationException {
}

export class UnknownInstrumentsException extends ApplicationException {
    instrumentIds: Integer[];
    onlyNeededForTests1: any;
    onlyNeededForTests2: Uint8Array;
    onlyNeededForTests3: any;
    static readonly TYPE_DESC = yass.classDesc(14, UnknownInstrumentsException,
        new yass.FieldDesc(1, 'instrumentIds', yass.LIST_DESC),
        new yass.FieldDesc(2, 'onlyNeededForTests1', null),
        new yass.FieldDesc(3, 'onlyNeededForTests2', yass.BYTES_DESC),
        new yass.FieldDesc(4, 'onlyNeededForTests3', null)
    );
}

export class Node {
    id: number;
    links: Node[];
    next: Node;
    static readonly TYPE_DESC = yass.classDesc(15, Node,
        new yass.FieldDesc(1, 'id', yass.NUMBER_DESC),
        new yass.FieldDesc(2, 'links', yass.LIST_DESC),
        new yass.FieldDesc(3, 'next', null)
    );
}

export namespace proxy {
    export interface EchoService {
        echo(value: any): Promise<any>;
    }
}
export namespace impl {
    export interface EchoService {
        echo(value: any): any;
    }
}
export namespace mapper {
    export const EchoService = new yass.MethodMapper(
        new yass.MethodMapping('echo', 0, false)
    );
}

export namespace proxy {
    export interface PriceEngine {
        subscribe(instrumentIds: Integer[]): Promise<void>;
    }
}
export namespace impl {
    export interface PriceEngine {
        subscribe(instrumentIds: Integer[]): void;
    }
}
export namespace mapper {
    export const PriceEngine = new yass.MethodMapper(
        new yass.MethodMapping('subscribe', 0, false)
    );
}

export namespace proxy {
    export interface PriceListener {
        newPrices(prices: Price[]): void;
    }
}
export namespace impl {
    export interface PriceListener {
        newPrices(prices: Price[]): void;
    }
}
export namespace mapper {
    export const PriceListener = new yass.MethodMapper(
        new yass.MethodMapping('newPrices', 0, true)
    );
}

export namespace instrument {
    export namespace proxy {
        export interface InstrumentService {
            getInstruments(): Promise<Instrument[]>;
            showOneWay(testBoolean: boolean, testInt: Integer): void;
        }
    }
    export namespace impl {
        export interface InstrumentService {
            getInstruments(): Instrument[];
            showOneWay(testBoolean: boolean, testInt: Integer): void;
        }
    }
    export namespace mapper {
        export const InstrumentService = new yass.MethodMapper(
            new yass.MethodMapping('getInstruments', 0, false),
            new yass.MethodMapping('showOneWay', 1, true)
        );
    }
}

export namespace initiator {
    export const priceListener = new yass.ContractId<proxy.PriceListener, impl.PriceListener>(0, mapper.PriceListener);
    export const echoService = new yass.ContractId<proxy.EchoService, impl.EchoService>(1, mapper.EchoService);
}

export namespace acceptor {
    export const priceEngine = new yass.ContractId<proxy.PriceEngine, impl.PriceEngine>(0, mapper.PriceEngine);
    export const instrumentService = new yass.ContractId<instrument.proxy.InstrumentService, instrument.impl.InstrumentService>(1, instrument.mapper.InstrumentService);
    export const echoService = new yass.ContractId<proxy.EchoService, impl.EchoService>(2, mapper.EchoService);
}

export const SERIALIZER = new yass.FastSerializer(
    IntegerHandler,
    Expiration,
    PriceKind,
    Price,
    instrument.stock.Stock,
    instrument.Bond,
    SystemException,
    UnknownInstrumentsException,
    Node
);
