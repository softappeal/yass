/// <reference path="../../tutorial/ts/contract"/>

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

namespace ioTest {

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

}

namespace enumTest {

    import PriceKind = contract.PriceKind;

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

}

namespace classTest {

    const stock = new contract.instrument.stock.Stock;
    stock.id = new Integer(1344);
    stock.name = "IBM";
    stock.paysDividend = true;
    log(stock);
    assert(stock instanceof contract.Instrument);
    assert(stock instanceof contract.instrument.stock.Stock);
    assert(!(stock instanceof contract.instrument.Bond));
    const exception = new contract.UnknownInstrumentsException;
    exception.instrumentIds = [new Integer(23), new Integer(454)];

}

namespace serializerTest {

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
    assert(copy(contract.PriceKind.ASK) === contract.PriceKind.ASK);
    assert(copy(contract.PriceKind.BID) === contract.PriceKind.BID);
    assert(copy(new Integer(123456)).value === 123456);
    assert(copy(new Integer(-987654)).value === -987654);

    function compare(array1: any[], array2: any[]): boolean {
        if (array1.length !== array2.length) {
            return false;
        }
        for (let i = 0; i < array1.length; i++) {
            const e1 = array1[i];
            const e2 = array2[i];
            if (e1 instanceof Integer) {
                if (e1.value !== e2.value) {
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
    stock.id = new Integer(1344);
    stock.name = "IBM";
    stock.paysDividend = true;
    stock = copy(stock);
    assert(stock.id.value === 1344);
    assert(stock.name === "IBM");
    assert(stock.paysDividend);
    stock.paysDividend = false;
    stock = copy(stock);
    assert(!stock.paysDividend);
    stock.paysDividend = null;
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
    e.instrumentIds = [new Integer(100), new Integer(200)];
    e = copy(e);
    assert(compare(e.instrumentIds, [new Integer(100), new Integer(200)]));

    let price = new contract.Price;
    price.instrumentId = new Integer(123);
    price.kind = contract.PriceKind.ASK;
    price.value = new Integer(999);
    price = copy(price);
    assert(price.instrumentId.value === 123);
    assert(price.kind === contract.PriceKind.ASK);
    assert(price.value.value === 999);

    const writer = new yass.Writer(1);
    writer.writeByte(123);
    writer.writeByte(0);
    writer.writeByte(210);
    const reader = new yass.Reader(copy(writer.getArray()));
    assert(reader.readByte() === 123);
    assert(reader.readByte() === 0);
    assert(reader.readByte() === 210);
    assert(reader.isEmpty());

    assert(new yass.Reader(copy(new yass.Writer(1).getArray())).isEmpty());

}

namespace interceptorTest {

    function i(id: number): yass.Interceptor {
        return (style, method, parameters, invocation) => {
            parameters[0] = (parameters[0] * 10) + id;
            return invocation();
        };
    }

    function invoke(...interceptors: yass.Interceptor[]): number {
        const parameters = [0];
        yass.composite.apply(null, interceptors)(null, null, parameters, () => "fkjskfjksjfl");
        return parameters[0];
    }

    assert(yass.composite() === yass.DIRECT);

    assert(invoke() === 0);
    assert(invoke(yass.DIRECT) === 0);
    assert(invoke(i(123)) === 123);
    assert(invoke(yass.DIRECT, i(123)) === 123);
    assert(invoke(i(123), yass.DIRECT) === 123);
    assert(invoke(i(9), i(8), i(7)) === 987);

}

const hostname = location.hostname;

namespace remoteTest {

    class Session extends yass.Session {
        constructor(connection: yass.Connection) {
            super(connection);
            log("constructor", this.isClosed());
        }
        protected server() {
            return new yass.Server(
                contract.initiator.echoService.service({echo: (value: any) => value})
            );
        }
        protected opened(): void {
            log("session opened", this.isClosed());
            const printer: yass.Interceptor = (style, method, parameters, invocation) => {
                function doLog(kind: string, data: any): void {
                    log("logger:", kind, yass.InvokeStyle[style], method, data);
                }
                doLog("entry", parameters);
                try {
                    const result = invocation();
                    doLog("exit", result);
                    return result;
                } catch (e) {
                    doLog("exception", e);
                    throw e;
                }
            };
            const instrumentService = this.proxy(contract.acceptor.instrumentService, printer);
            const priceEngine = this.proxy(contract.acceptor.priceEngine, printer);
            const echoService = this.proxy(contract.acceptor.echoService, printer);
            instrumentService.reload(false, new Integer(123));
            echoService.echo(null).then(result => assert(result === null));
            echoService.echo(undefined).then(result => assert(result === null));
            const stock = new contract.instrument.stock.Stock;
            stock.id = new Integer(123);
            stock.name = null;
            stock.paysDividend = undefined;
            echoService.echo(stock).then(result => {
                assert(result.id.value === 123);
                assert(result.name === undefined);
                assert(result.paysDividend === undefined);
            });
            echoService.echo(new Integer(12345678)).then(result =>  assert(result.value === 12345678));
            echoService.echo(new Integer(-87654321)).then(result  => assert(result.value === -87654321));
            echoService.echo(123.456e98).then(result => assert(result === 123.456e98));
            echoService.echo(-9.384762637432E-12).then(result => assert(result === -9.384762637432E-12));
            echoService.echo(new contract.Expiration(9, 8, 7)).then(result => {
                const expiration = <contract.Expiration>result;
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
            priceEngine.subscribe([new Integer(987654321)]).catch(exception => log("subscribe failed with", exception));
            setTimeout(() => this.close(), 2000);
        }
        protected closed(exception: any): void {
            log("session closed", this.isClosed(), exception);
        }
    }

    yass.connect(
        "ws://" + hostname + ":9090/tutorial",
        contract.SERIALIZER,
        connection => new Session(connection)
    );

}

namespace xhrTest {

    const proxyFactory = yass.xhr("http://" + hostname + ":9090/xhr", contract.SERIALIZER);
    const instrumentService = proxyFactory.proxy(contract.acceptor.instrumentService);
    const echoService = proxyFactory.proxy(contract.acceptor.echoService);
    assertThrown(() => instrumentService.reload(false, new Integer(123)));
    echoService.echo("echo").then(result => log("echo succeeded:", result));
    echoService.echo("throwRuntimeException").catch(error => log("throwRuntimeException failed:", error));

    assertThrown(() => yass.xhr("dummy://" + hostname + ":9090/xhr", contract.SERIALIZER).proxy(contract.acceptor.echoService).echo("echo1"));
    yass.xhr("http://" + hostname + ":9090/dummy", contract.SERIALIZER).proxy(contract.acceptor.echoService).echo("echo2").catch(error => log("echo2 failed:", error));
    yass.xhr("http://" + hostname + ":9999/xhr", contract.SERIALIZER).proxy(contract.acceptor.echoService).echo("echo3").catch(error => log("echo3 failed:", error));

}

log("done");
