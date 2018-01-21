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
    instrumentId: Integer | undefined | null;
    kind: PriceKind | undefined | null;
    value: Integer | undefined | null;
    static readonly TYPE_DESC = yass.classDesc(10, Price,
        new yass.FieldDesc(1, 'instrumentId', IntegerHandler.TYPE_DESC),
        new yass.FieldDesc(2, 'kind', PriceKind.TYPE_DESC),
        new yass.FieldDesc(3, 'value', IntegerHandler.TYPE_DESC)
    );
}

export abstract class Instrument {
    id: Integer | undefined | null;
    name: string | undefined | null;
}

export namespace instrument.stock {
    export class Stock extends Instrument {
        paysDividend: boolean | undefined | null;
        static readonly TYPE_DESC = yass.classDesc(11, Stock,
            new yass.FieldDesc(1, 'paysDividend', yass.BOOLEAN_DESC),
            new yass.FieldDesc(2, 'id', IntegerHandler.TYPE_DESC),
            new yass.FieldDesc(3, 'name', yass.STRING_DESC)
        );
    }
}

export namespace instrument {
    export class Bond extends Instrument {
        coupon: number | undefined | null;
        expiration: Expiration | undefined | null;
        static readonly TYPE_DESC = yass.classDesc(12, Bond,
            new yass.FieldDesc(1, 'coupon', yass.NUMBER_DESC),
            new yass.FieldDesc(2, 'expiration', Expiration.TYPE_DESC),
            new yass.FieldDesc(3, 'id', IntegerHandler.TYPE_DESC),
            new yass.FieldDesc(4, 'name', yass.STRING_DESC)
        );
    }
}

export class SystemException {
    message: string | undefined | null;
    static readonly TYPE_DESC = yass.classDesc(13, SystemException,
        new yass.FieldDesc(1, 'message', yass.STRING_DESC)
    );
}

export abstract class ApplicationException {
}

export class UnknownInstrumentsException extends ApplicationException {
    instrumentIds: Integer[] | undefined | null;
    onlyNeededForTests1: any | undefined | null;
    onlyNeededForTests2: Uint8Array | undefined | null;
    onlyNeededForTests3: any | undefined | null;
    static readonly TYPE_DESC = yass.classDesc(14, UnknownInstrumentsException,
        new yass.FieldDesc(1, 'instrumentIds', yass.LIST_DESC),
        new yass.FieldDesc(2, 'onlyNeededForTests1', null),
        new yass.FieldDesc(3, 'onlyNeededForTests2', yass.BYTES_DESC),
        new yass.FieldDesc(4, 'onlyNeededForTests3', null)
    );
}

export class Node {
    id: number | undefined | null;
    links: Node[] | undefined | null;
    next: Node | undefined | null;
    static readonly TYPE_DESC = yass.classDesc(15, Node,
        new yass.FieldDesc(1, 'id', yass.NUMBER_DESC),
        new yass.FieldDesc(2, 'links', yass.LIST_DESC),
        new yass.FieldDesc(3, 'next', null)
    );
}

export namespace generic {
    export class Pair<F, S> {
        first: F | undefined | null;
        second: S | undefined | null;
        static readonly TYPE_DESC = yass.classDesc(16, Pair,
            new yass.FieldDesc(1, 'first', null),
            new yass.FieldDesc(2, 'second', null)
        );
    }
}

export namespace generic {
    export class PairBoolBool extends generic.Pair<boolean, boolean> {
        static readonly TYPE_DESC = yass.classDesc(17, PairBoolBool,
            new yass.FieldDesc(1, 'first', null),
            new yass.FieldDesc(2, 'second', null)
        );
    }
}

export namespace generic {
    export class Triple<F, T> extends generic.Pair<F, boolean> {
        third: T | undefined | null;
        static readonly TYPE_DESC = yass.classDesc(18, Triple,
            new yass.FieldDesc(1, 'third', null),
            new yass.FieldDesc(2, 'first', null),
            new yass.FieldDesc(3, 'second', null)
        );
    }
}

export namespace generic {
    export class TripleWrapper {
        triple: generic.Triple<PriceKind, generic.Pair<string, generic.PairBoolBool[]>> | undefined | null;
        static readonly TYPE_DESC = yass.classDesc(19, TripleWrapper,
            new yass.FieldDesc(1, 'triple', null)
        );
    }
}

export namespace proxy {
    export interface EchoService {
        echo(value: any | undefined | null): Promise<any | undefined | null>;
    }
}
export namespace impl {
    export interface EchoService {
        echo(value: any | undefined | null): any | undefined | null;
    }
}
export namespace mapper {
    export const EchoService = new yass.MethodMapper(
        new yass.MethodMapping('echo', 0, false)
    );
}

export namespace proxy {
    export interface PriceEngine {
        subscribe(instrumentIds: Integer[] | undefined | null): Promise<void | undefined | null>;
    }
}
export namespace impl {
    export interface PriceEngine {
        subscribe(instrumentIds: Integer[] | undefined | null): void | undefined | null;
    }
}
export namespace mapper {
    export const PriceEngine = new yass.MethodMapper(
        new yass.MethodMapping('subscribe', 0, false)
    );
}

export namespace proxy {
    export interface PriceListener {
        newPrices(prices: Price[] | undefined | null): void;
    }
}
export namespace impl {
    export interface PriceListener {
        newPrices(prices: Price[] | undefined | null): void;
    }
}
export namespace mapper {
    export const PriceListener = new yass.MethodMapper(
        new yass.MethodMapping('newPrices', 0, true)
    );
}

export namespace generic {
    export namespace proxy {
        export interface GenericEchoService {
            echo(value: generic.Pair<boolean, generic.TripleWrapper> | undefined | null): Promise<generic.Pair<boolean, generic.TripleWrapper> | undefined | null>;
        }
    }
    export namespace impl {
        export interface GenericEchoService {
            echo(value: generic.Pair<boolean, generic.TripleWrapper> | undefined | null): generic.Pair<boolean, generic.TripleWrapper> | undefined | null;
        }
    }
    export namespace mapper {
        export const GenericEchoService = new yass.MethodMapper(
            new yass.MethodMapping('echo', 0, false)
        );
    }
}

export namespace instrument {
    export namespace proxy {
        export interface InstrumentService {
            getInstruments(): Promise<Instrument[] | undefined | null>;
            showOneWay(testBoolean: boolean | undefined | null, testInt: Integer | undefined | null): void;
        }
    }
    export namespace impl {
        export interface InstrumentService {
            getInstruments(): Instrument[] | undefined | null;
            showOneWay(testBoolean: boolean | undefined | null, testInt: Integer | undefined | null): void;
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
    export const genericEchoService = new yass.ContractId<generic.proxy.GenericEchoService, generic.impl.GenericEchoService>(3, generic.mapper.GenericEchoService);
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
    Node,
    generic.Pair,
    generic.PairBoolBool,
    generic.Triple,
    generic.TripleWrapper
);
