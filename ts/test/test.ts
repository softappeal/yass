import * as yass from "yass";
import {IntegerImpl} from "../tutorial/baseTypes-external";
import * as contract from "../tutorial/generated/contract";
import PriceKind = contract.PriceKind;
import * as Rx from "@reactivex/rxjs";

function log(...args: any[]): void {
    console.log.apply(console, args);
}

function assert(value: boolean): void {
    if (!value) {
        log("####### assert failed #######");
        throw new Error("assert failed");
    }
}

function assertThrown(action: () => void): void {
    let thrown = false;
    try {
        action();
    } catch (e) {
        thrown = true;
        log("expected error caught:", e);
    }
    assert(thrown);
}

assert(true);
assertThrown(() => {
    throw new Error("test")
});

function toArrayBuffer(byteArray: Uint8Array): ArrayBuffer {
    const arrayBuffer = new ArrayBuffer(byteArray.length);
    new Uint8Array(arrayBuffer).set(byteArray);
    return arrayBuffer;
}

function writer2reader(writer: yass.Writer): yass.Reader {
    return new yass.Reader(toArrayBuffer(writer.getArray()));
}

(function ioTest(): void {

    let writer = new yass.Writer(1);
    writer.writeByte(123);
    writer.writeByte(210);
    writer.writeInt(0);
    writer.writeInt(21);
    writer.writeInt(25658);
    writer.writeInt(-13);
    writer.writeInt(-344554);
    writer.writeInt(2147483647);
    writer.writeInt(-2147483648);
    writer.writeVarInt(0);
    writer.writeVarInt(21);
    writer.writeVarInt(25658);
    writer.writeVarInt(-13);
    writer.writeVarInt(-344554);
    writer.writeVarInt(2147483647);
    writer.writeVarInt(-2147483648);
    writer.writeZigZagInt(0);
    writer.writeZigZagInt(21);
    writer.writeZigZagInt(25658);
    writer.writeZigZagInt(-13);
    writer.writeZigZagInt(-344554);
    writer.writeZigZagInt(2147483647);
    writer.writeZigZagInt(-2147483648);
    assert(writer.getArray().length === 74);

    let reader = writer2reader(writer);
    assert(!reader.isEmpty());
    assert(reader.readByte() === 123);
    assert(reader.readByte() === 210);
    assert(reader.readInt() === 0);
    assert(reader.readInt() === 21);
    assert(reader.readInt() === 25658);
    assert(reader.readInt() === -13);
    assert(reader.readInt() === -344554);
    assert(reader.readInt() === 2147483647);
    assert(reader.readInt() === -2147483648);
    assert(reader.readVarInt() === 0);
    assert(reader.readVarInt() === 21);
    assert(reader.readVarInt() === 25658);
    assert(reader.readVarInt() === -13);
    assert(reader.readVarInt() === -344554);
    assert(reader.readVarInt() === 2147483647);
    assert(reader.readVarInt() === -2147483648);
    assert(reader.readZigZagInt() === 0);
    assert(reader.readZigZagInt() === 21);
    assert(reader.readZigZagInt() === 25658);
    assert(reader.readZigZagInt() === -13);
    assert(reader.readZigZagInt() === -344554);
    assert(reader.readZigZagInt() === 2147483647);
    assert(reader.readZigZagInt() === -2147483648);
    assert(reader.isEmpty());
    assertThrown(function () {
        reader.readByte();
    });

    writer = new yass.Writer(100);
    writer.writeByte(128);
    writer.writeByte(128);
    writer.writeByte(128);
    writer.writeByte(128);
    writer.writeByte(128);
    reader = writer2reader(writer);
    assertThrown(function () {
        reader.readVarInt();
    });

    function utf8(bytes: number, value: string): void {
        assert(yass.Writer.calcUtf8bytes(value) === bytes);
        const writer = new yass.Writer(100);
        writer.writeUtf8(value);
        assert(writer.getArray().length === bytes);
        const reader = writer2reader(writer);
        assert(reader.readUtf8(bytes) === value);
        assert(reader.isEmpty());
    }

    utf8(2, "><");
    utf8(3, ">\u0000<");
    utf8(3, ">\u0001<");
    utf8(3, ">\u0012<");
    utf8(3, ">\u007F<");
    utf8(4, ">\u0080<");
    utf8(4, ">\u0234<");
    utf8(4, ">\u07FF<");
    utf8(5, ">\u0800<");
    utf8(5, ">\u4321<");
    utf8(5, ">\uFFFF<");

})();

(function enumTest() {

    const ask = PriceKind.ASK;
    log(ask);
    assert(ask.number === 1);
    assert(ask.name === "ASK");
    assert(ask === PriceKind.ASK);
    assert(ask !== PriceKind.BID);
    log(">" + ask + "<");
    assert(PriceKind.VALUES.length == 2);
    assert(PriceKind.VALUES[PriceKind.BID.number] === PriceKind.BID);
    assert(PriceKind.VALUES[PriceKind.ASK.number] === PriceKind.ASK);

})();

(function classTest() {

    const stock = new contract.instrument.stock.Stock;
    stock.id = new IntegerImpl(1344);
    stock.name = "IBM";
    stock.paysDividend = true;
    log(stock);
    assert(stock instanceof contract.Instrument);
    assert(stock instanceof contract.instrument.stock.Stock);
    assert(!(stock instanceof contract.instrument.Bond));
    const exception = new contract.UnknownInstrumentsException;
    exception.instrumentIds = [new IntegerImpl(23), new IntegerImpl(454)];

})();

(function serializerTest() {

    function copy(value: any): any {
        const writer = new yass.Writer(100);
        contract.SERIALIZER.write(value, writer);
        const reader = writer2reader(writer);
        const result = contract.SERIALIZER.read(reader);
        assert(reader.isEmpty());
        return result;
    }

    assert(copy(null) === null);
    assert(copy(true));
    assert(!copy(false));
    assert(copy(1234567) === 1234567);
    assert(copy(-1234567) === -1234567);
    assert(copy("") === "");
    assert(copy("blabli") === "blabli");
    assert(copy(PriceKind.ASK) === PriceKind.ASK);
    assert(copy(PriceKind.BID) === PriceKind.BID);
    assert(copy(new IntegerImpl(123456)).get() === 123456);
    assert(copy(new IntegerImpl(-987654)).get() === -987654);

    function compare(array1: any[], array2: any[]): boolean {
        if (array1.length !== array2.length) {
            return false;
        }
        for (let i = 0; i < array1.length; i++) {
            const e1 = array1[i];
            const e2 = array2[i];
            if (e1 instanceof IntegerImpl) {
                if (e1.get() !== e2.get()) {
                    return false;
                }
            } else if (e1 !== e2) {
                return false;
            }
        }
        return true;
    }

    assert(Array.isArray(copy([])));
    assert(copy([]).length === 0);
    assert(compare(copy([12]), [12]));
    assert(compare(copy([12, true, "bla"]), [12, true, "bla"]));

    let stock = new contract.instrument.stock.Stock;
    stock.id = new IntegerImpl(1344);
    stock.name = "IBM";
    stock.paysDividend = true;
    stock = copy(stock);
    assert(stock.id.get() === 1344);
    assert(stock.name === "IBM");
    assert(stock.paysDividend);
    stock.paysDividend = false;
    stock = copy(stock);
    assert(!stock.paysDividend);
    stock.paysDividend = null!;
    stock = copy(stock);
    assert(stock.paysDividend === undefined);

    let bond = new contract.instrument.Bond;
    bond.coupon = 3.5;
    bond.expiration = new contract.Expiration(2013, 2, 20);
    bond = copy(bond);
    assert(bond.coupon === 3.5);
    assert(bond.expiration.year === 2013);
    assert(bond.expiration.month === 2);
    assert(bond.expiration.day === 20);

    let e = new contract.UnknownInstrumentsException;
    e.instrumentIds = [new IntegerImpl(100), new IntegerImpl(200)];
    e = copy(e);
    assert(compare(e.instrumentIds, [new IntegerImpl(100), new IntegerImpl(200)]));

    let price = new contract.Price;
    price.instrumentId = new IntegerImpl(123);
    price.kind = PriceKind.ASK;
    price.value = new IntegerImpl(999);
    price = copy(price);
    assert(price.instrumentId.get() === 123);
    assert(price.kind === PriceKind.ASK);
    assert(price.value.get() === 999);

    let writer = new yass.Writer(1);
    writer.writeByte(123);
    writer.writeByte(0);
    writer.writeByte(210);
    let reader = new yass.Reader(copy(writer.getArray()));
    assert(reader.readByte() === 123);
    assert(reader.readByte() === 0);
    assert(reader.readByte() === 210);
    assert(reader.isEmpty());

    assert(new yass.Reader(copy(new yass.Writer(1).getArray())).isEmpty());

    let exception = new contract.UnknownInstrumentsException;
    exception.onlyNeededForTests1 = 123456;
    writer = new yass.Writer(1);
    writer.writeByte(121);
    writer.writeByte(0);
    writer.writeByte(250);
    exception.onlyNeededForTests2 = writer.getArray();
    exception = copy(exception);
    assert(exception.onlyNeededForTests1 === 123456);
    reader = new yass.Reader(exception.onlyNeededForTests2);
    assert(reader.readByte() === 121);
    assert(reader.readByte() === 0);
    assert(reader.readByte() === 250);
    assert(reader.isEmpty());

})();

const hostname = "localhost";

function doLog(context: yass.SimpleInterceptorContext, kind: string, data: any): void {
    log("logger:", kind, context.id, context.methodMapping.method, data);
}
const printer: yass.Interceptor<yass.SimpleInterceptorContext> = {
    entry(methodMapping: yass.MethodMapping, parameters: any[]): yass.SimpleInterceptorContext {
        const context = new yass.SimpleInterceptorContext(methodMapping, parameters);
        doLog(context, "entry", parameters);
        return context;
    },
    exit(context: yass.SimpleInterceptorContext, result: any): void {
        doLog(context, "exit", result);
    },
    exception(context: yass.SimpleInterceptorContext, exception: any): void {
        doLog(context, "exception", exception);
    },
    resolved(context: yass.SimpleInterceptorContext): void {
        doLog(context, "resolved", "");
    }
};

(function remoteTest() {

    class Session extends yass.Session {
        constructor(connection: yass.Connection) {
            super(connection);
            log("constructor", this.isClosed());
        }
        protected server() {
            return new yass.Server(
                contract.initiator.echoService.service({
                    echo: (value: any) => {
                        if ("throwRuntimeException" === value) {
                            const e = new contract.SystemException;
                            e.message = value;
                            throw e;
                        }
                        return value;
                    }
                })
            );
        }
        protected opened(): void {
            log("session opened", this.isClosed());
            const instrumentService = this.proxy(contract.acceptor.instrumentService, printer);
            const priceEngine = this.proxy(contract.acceptor.priceEngine, printer);
            const echoService = this.proxy(contract.acceptor.echoService, printer);
            instrumentService.showOneWay(false, new IntegerImpl(123));
            echoService.echo(null).then(result => assert(result === null));
            echoService.echo(undefined).then(result => assert(result === null));
            echoService.echo(true).then(result => assert(result === true));
            echoService.echo(false).then(result => assert(result === false));
            const stock = new contract.instrument.stock.Stock;
            stock.id = new IntegerImpl(123);
            stock.name = null!;
            stock.paysDividend = false;
            echoService.echo(stock).then(result => {
                assert(result.id.get() === 123);
                assert(result.name === undefined);
                assert(result.paysDividend === false);
            });
            echoService.echo(new IntegerImpl(12345678)).then(result =>  assert(result.get() === 12345678));
            echoService.echo(new IntegerImpl(-87654321)).then(result  => assert(result.get() === -87654321));
            echoService.echo(123.456e98).then(result => assert(result === 123.456e98));
            echoService.echo(-9.384762637432E-12).then(result => assert(result === -9.384762637432E-12));
            echoService.echo(new contract.Expiration(9, 8, 7)).then(expiration => {
                log("then", expiration);
                assert(expiration.year === 9);
                assert(expiration.month === 8);
                assert(expiration.day === 7);
            });
            const writer = new yass.Writer(1);
            writer.writeByte(123);
            writer.writeByte(0);
            writer.writeByte(210);
            echoService.echo(writer.getArray()).then(result => {
                const reader = new yass.Reader(toArrayBuffer(result));
                assert(reader.readByte() === 123);
                assert(reader.readByte() === 0);
                assert(reader.readByte() === 210);
                assert(reader.isEmpty());
            });
            priceEngine.subscribe([new IntegerImpl(987654321)]).catch(exception => log("subscribe failed with", exception));
            setTimeout(() => this.close(), 2000);
        }
        protected closed(exception: any): void {
            log("session closed", this.isClosed(), exception);
        }
    }

    yass.connect(
        "ws://" + hostname + ":9090/ws",
        contract.SERIALIZER,
        connection => new Session(connection)
    );

    class Session2 extends yass.Session { // needed for more coverage (exception cases)
        constructor(connection: yass.Connection) {
            super(connection);
        }
        protected closed(exception: any): void {
            log("session 2 closed", exception);
        }
    }

    yass.connect(
        "ws://" + hostname + ":9090/ws",
        contract.SERIALIZER,
        connection => new Session2(connection)
    );

})();

(async function xhrTest() {

    const proxyFactory = yass.xhr("http://" + hostname + ":9090/xhr", contract.SERIALIZER);
    const echoService = proxyFactory.proxy(contract.acceptor.echoService, printer);

    log("echo succeeded:", await echoService.echo("echo"));
    try {
        await echoService.echo("throwRuntimeException");
    } catch (e) {
        log("throwRuntimeException failed:", e)
    }
    yass.xhr("http://" + hostname + ":9090/xhr", contract.SERIALIZER, 500).proxy(contract.acceptor.echoService).echo("timeout").catch(error => log("timeout failed:", error));

    const node1 = new contract.Node;
    const node2 = new contract.Node;
    node1.id = 1;
    node2.id = 2;
    node1.next = node2;
    log("echo succeeded:", await echoService.echo(node1));

    function subscribe(value: string): Rx.Subscription {
        return Rx.Observable.fromPromise(echoService.echo(value)).subscribe({
            next: function (v: any): void {
                log("next:", value, "-", v);
            },
            error: function (err: any): void {
                log("error:", value, "-", err);
            },
            complete: function (): void {
                log("complete:", value);
            }
        });
    }
    subscribe("throwRuntimeException");
    subscribe("unsubscribe").unsubscribe();
    subscribe("regular");
    /*
        logger: entry 16 echo [ 'throwRuntimeException' ]
        logger: entry 17 echo [ 'unsubscribe' ]
        logger: entry 18 echo [ 'regular' ]

        logger: exception 16 echo SystemException { message: 'java.lang.RuntimeException: throwRuntimeException' }
        error: throwRuntimeException - SystemException { message: 'java.lang.RuntimeException: throwRuntimeException' }
        logger: resolved 16 echo

        logger: exit 17 echo unsubscribe
        logger: resolved 17 echo

        logger: exit 18 echo regular
        next: regular - regular
        complete: regular
        logger: resolved 18 echo
     */

})();

log("done");
