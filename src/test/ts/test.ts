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

function assertThrown(action: ()=>void): void {
    var thrown = false;
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
    var arrayBuffer = new ArrayBuffer(byteArray.length);
    new Uint8Array(arrayBuffer).set(byteArray);
    return arrayBuffer;
}

function writer2reader(writer: yass.Writer): yass.Reader {
    return new yass.Reader(toArrayBuffer(writer.getArray()));
}

module ioTest {

    var writer = new yass.Writer(1);
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

    var reader = writer2reader(writer);
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
        var writer = new yass.Writer(100);
        writer.writeUtf8(value);
        assert(writer.getArray().length === bytes);
        var reader = writer2reader(writer);
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

module enumTest {

    var ask = contract.PriceType.ASK;
    log(ask);
    assert(ask.number === 1);
    assert(ask.name === "ASK");
    assert(ask === contract.PriceType.ASK);
    assert(ask !== contract.PriceType.BID);

}

module classTest {

    var stock = new contract.instrument.stock.Stock;
    stock.id = 1344;
    stock.name = "IBM";
    stock.paysDividend = true;
    log(stock);
    assert(stock instanceof contract.Instrument);
    assert(stock instanceof contract.instrument.stock.Stock);
    assert(!(stock instanceof contract.instrument.Bond));
    var exception = new contract.UnknownInstrumentsException;
    exception.instrumentIds = [23, 454];

}

module serializerTest {

    function copy(value: any): any {
        var writer = new yass.Writer(100);
        contract.SERIALIZER.write(value, writer);
        var reader = writer2reader(writer);
        var result = contract.SERIALIZER.read(reader);
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
    assert(copy(contract.PriceType.ASK) === contract.PriceType.ASK);
    assert(copy(contract.PriceType.BID) === contract.PriceType.BID);
    assert(copy(new contract.instrument.stock.JsDouble(123.456e98)).d === 123.456e98);
    assert(copy(new contract.instrument.stock.JsDouble(-9.384762637432E-12)).d === -9.384762637432E-12);

    function compare(array1: any[], array2: any[]): boolean {
        if (array1.length !== array2.length) {
            return false;
        }
        for (var i = 0; i < array1.length; i++) {
            if (array1[i] !== array2[i]) {
                return false;
            }
        }
        return true;
    }

    assert(Array.isArray(copy([])));
    assert(copy([]).length === 0);
    assert(compare(copy([12]), [12]));
    assert(compare(copy([12, true, "bla"]), [12, true, "bla"]));

    var stock = new contract.instrument.stock.Stock;
    stock.id = 1344;
    stock.name = "IBM";
    stock.paysDividend = true;
    stock = copy(stock);
    assert(stock.id === 1344);
    assert(stock.name === "IBM");
    assert(stock.paysDividend);
    stock.paysDividend = false;
    stock = copy(stock);
    assert(!stock.paysDividend);
    stock.paysDividend = null;
    stock = copy(stock);
    assert(stock.paysDividend === undefined);

    var bond = new contract.instrument.Bond;
    bond.coupon = new contract.instrument.stock.JsDouble(3.5);
    bond.expiration = new contract.Expiration(2013, 2, 20);
    bond = copy(bond);
    assert(bond.coupon.d === 3.5);
    assert(bond.expiration.year === 2013);
    assert(bond.expiration.month === 2);
    assert(bond.expiration.day === 20);

    var e = new contract.UnknownInstrumentsException;
    e.instrumentIds = [100, 200];
    e.comment = bond;
    e.dump = new yass.Writer(100).getArray();
    e = copy(e);
    assert(compare(e.instrumentIds, [100, 200]));
    assert(e.comment.coupon.d === 3.5);
    assert(new yass.Reader(toArrayBuffer(e.dump)).isEmpty());

    var price = new contract.Price;
    price.instrumentId = 123;
    price.type = contract.PriceType.ASK;
    price.value = 999;
    price = copy(price);
    assert(price.instrumentId === 123);
    assert(price.type === contract.PriceType.ASK);
    assert(price.value === 999);

    var writer = new yass.Writer(1);
    writer.writeByte(123);
    writer.writeByte(0);
    writer.writeByte(210);
    var reader = new yass.Reader(copy(writer.getArray()));
    assert(reader.readByte() === 123);
    assert(reader.readByte() === 0);
    assert(reader.readByte() === 210);
    assert(reader.isEmpty());

    assert(new yass.Reader(copy(new yass.Writer(1).getArray())).isEmpty());

}

module interceptorTest {

    function i(id: number): yass.Interceptor {
        return (style, method, parameters, proceed) => {
            parameters[0] = (parameters[0] * 10) + id;
            return proceed();
        };
    }

    function invoke(...interceptors: yass.Interceptor[]): number {
        var parameters = [0];
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

module remoteTest {

    function sessionFactory(sessionInvokerFactory: yass.SessionInvokerFactory): yass.Session {
        return {
            opened: function () {
                log("session opened");
                var printer: yass.Interceptor = (style, method, parameters, proceed) => {
                    log("printer:", yass.InvokeStyle[style], method, parameters);
                    return proceed();
                };
                var echoService = sessionInvokerFactory.invoker(contract.ServerServices.EchoService)(printer);
                echoService.echo(null).then(
                    result => assert(result === null)
                );
                echoService.echo(undefined).then(
                    result => assert(result === null)
                );
                var stock = new contract.instrument.stock.Stock;
                stock.id = 123;
                stock.name = null;
                stock.paysDividend = undefined;
                echoService.echo(stock).then(
                    result => {
                        assert(result.id === 123);
                        assert(result.name === undefined);
                        assert(result.paysDividend === undefined);
                    }
                );
                echoService.echo(12345678).then(
                    result =>  assert(result === 12345678)
                );
                echoService.echo(-87654321).then(
                    result  => assert(result === -87654321)
                );
                echoService.echo(new contract.instrument.stock.JsDouble(123.456e98)).then(
                    result => assert((<contract.instrument.stock.JsDouble>result).d === 123.456e98)
                );
                echoService.echo(new contract.instrument.stock.JsDouble(-9.384762637432E-12)).then(
                    result => assert((<contract.instrument.stock.JsDouble>result).d === -9.384762637432E-12)
                );
                echoService.echo(new contract.Expiration(9, 8, 7)).then(
                    result => {
                        var expiration = <contract.Expiration>result;
                        assert(expiration.year === 9);
                        assert(expiration.month === 8);
                        assert(expiration.day === 7);
                    }
                );
                var writer = new yass.Writer(1);
                writer.writeByte(123);
                writer.writeByte(0);
                writer.writeByte(210);
                echoService.echo(writer.getArray()).then(
                    result => {
                        var reader = new yass.Reader(toArrayBuffer(result));
                        assert(reader.readByte() === 123);
                        assert(reader.readByte() === 0);
                        assert(reader.readByte() === 210);
                        assert(reader.isEmpty());
                    }
                );
                setTimeout(() => sessionInvokerFactory.close(), 1000);
            },
            closed: function (exception) {
                log("session closed", exception);
            }
        };
    }

    yass.connect(
        "ws://localhost:9090/tutorial",
        contract.SERIALIZER,
        yass.server(
            new yass.Service(contract.ClientServices.EchoService, {echo: (value: any) => value})
        ),
        sessionFactory,
        () => log("connectFailed")
    );

}
