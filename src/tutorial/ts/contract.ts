/// <reference path='baseTypes'/>

module contract {

    export var GENERATED_BY_YASS_VERSION = 'null';

    export class PriceType extends yass.Enum {
        static BID = new PriceType(0, 'BID');
        static ASK = new PriceType(1, 'ASK');
        static TYPE_DESC = yass.enumDesc(9, PriceType);
    }

    export class Price extends yass.Type {
        instrumentId: string;
        type: PriceType;
        value: number;
        static TYPE_DESC = yass.classDesc(10, Price,
            new yass.FieldDesc(1, 'instrumentId', yass.STRING_DESC),
            new yass.FieldDesc(2, 'type', PriceType.TYPE_DESC),
            new yass.FieldDesc(3, 'value', yass.INTEGER_DESC)
        );
    }

    export class Instrument extends yass.Type {
        id: string;
        name: string;
    }

    export module instrument.stock {
        export class Stock extends Instrument {
            paysDividend: boolean;
            static TYPE_DESC = yass.classDesc(11, Stock,
                new yass.FieldDesc(1, 'id', yass.STRING_DESC),
                new yass.FieldDesc(2, 'name', yass.STRING_DESC),
                new yass.FieldDesc(3, 'paysDividend', yass.BOOLEAN_DESC)
            );
        }
    }

    export module instrument {
        export class Bond extends Instrument {
            coupon: instrument.stock.JsDouble;
            expiration: Expiration;
            static TYPE_DESC = yass.classDesc(12, Bond,
                new yass.FieldDesc(1, 'coupon', instrument.stock.JsDouble.TYPE_DESC),
                new yass.FieldDesc(2, 'expiration', Expiration.TYPE_DESC),
                new yass.FieldDesc(3, 'id', yass.STRING_DESC),
                new yass.FieldDesc(4, 'name', yass.STRING_DESC)
            );
        }
    }

    export class UnknownInstrumentsException extends yass.Type {
        comment: any;
        dump: Uint8Array;
        instrumentIds: string[];
        static TYPE_DESC = yass.classDesc(13, UnknownInstrumentsException,
            new yass.FieldDesc(1, 'comment', null),
            new yass.FieldDesc(2, 'dump', yass.BYTES_DESC),
            new yass.FieldDesc(3, 'instrumentIds', yass.LIST_DESC)
        );
    }

    export class Trade extends yass.Type {
        amount: number;
        instrument: Instrument;
        static TYPE_DESC = yass.classDesc(14, Trade,
            new yass.FieldDesc(1, 'amount', yass.INTEGER_DESC),
            new yass.FieldDesc(2, 'instrument', null)
        );
    }

    export interface EchoService {
        echo(value: any): any;
    }
    export interface EchoService_PROXY {
        echo(value: any): Promise<any>;
    }
    export var EchoService_MAPPER = new yass.MethodMapper<EchoService>(
        new yass.MethodMapping('echo', 0, false)
    );

    export interface PriceEngine {
        subscribe(instrumentIds: string[]): void;
    }
    export interface PriceEngine_PROXY {
        subscribe(instrumentIds: string[]): Promise<void>;
    }
    export var PriceEngine_MAPPER = new yass.MethodMapper<PriceEngine>(
        new yass.MethodMapping('subscribe', 0, false)
    );

    export interface PriceListener {
        newPrices(prices: Price[]): void;
    }
    export interface PriceListener_PROXY {
        newPrices(prices: Price[]): void;
    }
    export var PriceListener_MAPPER = new yass.MethodMapper<PriceListener>(
        new yass.MethodMapping('newPrices', 0, true)
    );

    export module instrument {
        export interface InstrumentService {
            getInstruments(): Instrument[];
            reload(testBoolean: boolean, testInt: number): void;
        }
        export interface InstrumentService_PROXY {
            getInstruments(): Promise<Instrument[]>;
            reload(testBoolean: boolean, testInt: number): void;
        }
        export var InstrumentService_MAPPER = new yass.MethodMapper<InstrumentService>(
            new yass.MethodMapping('getInstruments', 0, false),
            new yass.MethodMapping('reload', 1, true)
        );
    }

    export module ClientServices {
        export var PriceListener = new yass.ContractId<PriceListener, PriceListener_PROXY>(0, PriceListener_MAPPER);
        export var EchoService = new yass.ContractId<EchoService, EchoService_PROXY>(1, EchoService_MAPPER);
    }

    export module ServerServices {
        export var PriceEngine = new yass.ContractId<PriceEngine, PriceEngine_PROXY>(0, PriceEngine_MAPPER);
        export var InstrumentService = new yass.ContractId<instrument.InstrumentService, instrument.InstrumentService_PROXY>(1, instrument.InstrumentService_MAPPER);
        export var EchoService = new yass.ContractId<EchoService, EchoService_PROXY>(2, EchoService_MAPPER);
    }

    export var SERIALIZER = new yass.JsFastSerializer(
        Expiration,
        instrument.stock.JsDouble,
        PriceType,
        Price,
        instrument.stock.Stock,
        instrument.Bond,
        UnknownInstrumentsException,
        Trade
    );

}
