import * as yass from "../yass";
import {IntegerImpl} from "../tutorial/baseTypes-external";
import * as contract from "../tutorial/generated/contract";
import PriceKind = contract.PriceKind;

function log(...args: any): void {
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
    return byteArray.slice().buffer;
}

function writer2reader(writer: yass.Writer): yass.Reader {
    return new yass.Reader(toArrayBuffer(writer.getArray()));
}

(function ioTest() {

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
        const codePoints = yass.Writer.toCodePoints(value);
        assert(yass.Writer.calcUtf8bytes(codePoints) === bytes);
        const writer = new yass.Writer(100);
        writer.writeUtf8(codePoints);
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
    utf8(5, ">\u7FFF<");
    utf8(6, ">\u8000<");
    utf8(6, ">\uFFFF<");
    utf8(6, ">\u{10400}<");
    utf8(6, ">\u{10FFFF}<");

})();

(function enumTest() {

    const ask = PriceKind.ASK;
    log(ask);
    assert(ask.number === 1);
    assert(ask.name === "ASK");
    assert(ask === PriceKind.ASK);
    assert(ask !== PriceKind.BID);
    log(">" + ask + "<");
    assert(PriceKind.VALUES.length === 2);
    assert(PriceKind.VALUES[PriceKind.BID.number] === PriceKind.BID);
    assert(PriceKind.VALUES[PriceKind.ASK.number] === PriceKind.ASK);

})();

(function classTest() {

    const stock = new contract.instrument.stock.Stock();
    stock.id = new IntegerImpl(1344);
    stock.name = "IBM";
    stock.paysDividend = true;
    log(stock);
    assert(stock instanceof contract.Instrument);
    assert(stock instanceof contract.instrument.stock.Stock);
    assert(!(stock instanceof contract.instrument.Bond));
    const exception = new contract.UnknownInstrumentsException();
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

    let stock = new contract.instrument.stock.Stock();
    stock.id = new IntegerImpl(1344);
    stock.name = "IBM";
    stock.paysDividend = true;
    stock = copy(stock);
    assert(stock.id!.get() === 1344);
    assert(stock.name === "IBM");
    assert(stock.paysDividend!);
    stock.paysDividend = false;
    stock = copy(stock);
    assert(!stock.paysDividend);
    stock.paysDividend = null;
    stock = copy(stock);
    assert(stock.paysDividend === undefined);

    let bond = new contract.instrument.Bond();
    bond.coupon = 3.5;
    bond.expiration = new contract.Expiration(2013);
    bond = copy(bond);
    assert(bond.coupon === 3.5);
    assert(bond.expiration!.year === 2013);

    let e = new contract.UnknownInstrumentsException();
    e.instrumentIds = [new IntegerImpl(100), new IntegerImpl(200)];
    e = copy(e);
    assert(compare(e.instrumentIds!, [new IntegerImpl(100), new IntegerImpl(200)]));

    let price = new contract.Price();
    price.instrumentId = new IntegerImpl(123);
    price.kind = PriceKind.ASK;
    price.value = new IntegerImpl(999);
    price = copy(price);
    assert(price.instrumentId!.get() === 123);
    assert(price.kind === PriceKind.ASK);
    assert(price.value!.get() === 999);

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

    let exception = new contract.UnknownInstrumentsException();
    exception.onlyNeededForTests1 = 123456;
    writer = new yass.Writer(1);
    writer.writeByte(121);
    writer.writeByte(0);
    writer.writeByte(250);
    exception.onlyNeededForTests2 = writer.getArray();
    exception = copy(exception);
    assert(exception.onlyNeededForTests1 === 123456);
    reader = new yass.Reader(exception.onlyNeededForTests2!);
    assert(reader.readByte() === 121);
    assert(reader.readByte() === 0);
    assert(reader.readByte() === 250);
    assert(reader.isEmpty());

})();

(function contextMessageSerializerTest() {
    const contextMessageSerializer = new yass.ContextMessageSerializer(
        new yass.FastSerializer(),
        new yass.MessageSerializer(contract.SERIALIZER)
    );
    const writer = new yass.Writer(100);
    contextMessageSerializer.context = "hello";
    contextMessageSerializer.write(new yass.ValueReply(null), writer);
    assert(contextMessageSerializer.context === null);
    const reader = writer2reader(writer);
    assert(contextMessageSerializer.read(reader) instanceof yass.ValueReply);
    assert(reader.isEmpty());
    assert(contextMessageSerializer.context === "hello");
})();

const hostname = "localhost";

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
                            const e = new contract.SystemException();
                            e.details = value;
                            throw e;
                        }
                        return value;
                    }
                })
            );
        }
        private static ID = 0;
        protected opened(): void {
            log("session opened", this.isClosed());
            function doLog(invocation: yass.AbstractInvocation, kind: string, data: any): void {
                log("logger:", kind, invocation.context, invocation.methodMapping.method, data);
            }
            const printer: yass.Interceptor = {
                entry(invocation: yass.AbstractInvocation): void {
                    invocation.context = Session.ID++;
                    doLog(invocation, "entry", invocation.parameters);
                },
                exit(invocation: yass.AbstractInvocation, result: any): void {
                    doLog(invocation, "exit", result);
                },
                exception(invocation: yass.AbstractInvocation, exception: any): void {
                    doLog(invocation, "exception", exception);
                },
                resolved(invocation: yass.AbstractInvocation): void {
                    doLog(invocation, "resolved", "");
                }
            };
            const instrumentService = this.proxy(contract.acceptor.instrumentService, printer);
            const priceEngine = this.proxy(contract.acceptor.priceEngine, printer);
            const echoService = this.proxy(contract.acceptor.echoService, printer);
            const genericEchoService = this.proxy(contract.acceptor.genericEchoService, printer);
            instrumentService.showOneWay(false, new IntegerImpl(123));
            echoService.echo(null).then(result => assert(result === null));
            echoService.echo(undefined).then(result => assert(result === null));
            echoService.echo(true).then(result => assert(result === true));
            echoService.echo(false).then(result => assert(result === false));
            const stock = new contract.instrument.stock.Stock();
            stock.id = new IntegerImpl(123);
            stock.name = null;
            stock.paysDividend = false;
            echoService.echo(stock).then(result => {
                assert(result.id.get() === 123);
                assert(result.name === undefined);
                assert(result.paysDividend === false);
            });
            echoService.echo(new IntegerImpl(12345678)).then(result => assert(result.get() === 12345678));
            echoService.echo(new IntegerImpl(-87654321)).then(result => assert(result.get() === -87654321));
            echoService.echo(123.456e98).then(result => assert(result === 123.456e98));
            echoService.echo(-9.384762637432E-12).then(result => assert(result === -9.384762637432E-12));
            echoService.echo(new contract.Expiration(9)).then(expiration => {
                log("then", expiration);
                assert(expiration.year === 9);
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

            const pairBoolBool1 = new contract.generic.PairBoolBool();
            pairBoolBool1.first = true;
            pairBoolBool1.second = false;
            const pairBoolBool2 = new contract.generic.PairBoolBool();
            pairBoolBool2.first = false;
            pairBoolBool2.second = true;
            const pair1 = new contract.generic.Pair<string, contract.generic.PairBoolBool[]>();
            pair1.first = "hello";
            pair1.second = [pairBoolBool1, pairBoolBool2];
            const triple = new contract.generic.Triple<PriceKind, contract.generic.Pair<string, contract.generic.PairBoolBool[]>>();
            triple.first = contract.PriceKind.ASK;
            triple.second = true;
            triple.third = pair1;
            const tripleWrapper = new contract.generic.TripleWrapper();
            tripleWrapper.triple = triple;
            const pair2 = new contract.generic.Pair<boolean, contract.generic.TripleWrapper>();
            pair2.first = true;
            pair2.second = tripleWrapper;
            genericEchoService.echo(pair2).then(result => {
                assert(result!.first!);
                const triple = result!.second!.triple;
                assert(triple!.first === contract.PriceKind.ASK);
                assert(triple!.second!);
                const pair = triple!.third;
                assert(pair!.first === "hello");
                assert(pair!.second!.length === 2);
                const pair1 = pair!.second![0];
                const pair2 = pair!.second![1];
                assert(pair1.first! && !pair1.second!);
                assert(!pair2.first! && pair2.second!);
                log("echoGeneric:", result);
            });

            priceEngine.subscribe([new IntegerImpl(987654321)]).catch(exception => log("subscribe failed with", exception));
            setTimeout(() => this.close(), 2000);
        }
        protected closed(exception: any): void {
            log("session closed", this.isClosed(), exception);
        }
    }

    yass.connect(
        new WebSocket("ws://" + hostname + ":9090/ws"),
        yass.packetSerializer(contract.SERIALIZER),
        connection => new Session(connection)
    );

    const ws = new WebSocket("ws://" + hostname + ":9999/ws");
    ws.onerror = () => {
        log("connection failed");
    };
    yass.connect(
        ws,
        yass.packetSerializer(contract.SERIALIZER),
        connection => new Session(connection)
    );

})();

(async function xhrTest() {

    const messageSerializer = new yass.MessageSerializer(contract.SERIALIZER);
    const proxyFactory = new yass.XhrClient("http://" + hostname + ":9090/xhr", messageSerializer);
    const echoService = proxyFactory.proxy(contract.acceptor.echoService);

    log("echo succeeded:", await echoService.echo("echo \u{10400}"));
    try {
        await echoService.echo("throwRuntimeException");
    } catch (e) {
        log("throwRuntimeException failed:", e)
    }

    new yass.XhrClient("http://" + hostname + ":9090/xhr", messageSerializer, 500)
        .proxy(contract.acceptor.echoService)
        .echo("timeout")
        .catch(error => log("timeout failed:", error));

    class RequestStatusException {
        constructor(public readonly status: number) {
            // empty
        }
    }
    new yass.XhrClient(
        "http://" + hostname + ":9090/xhr",
        messageSerializer,
        0,
        xhr => {
            log("checkXhr:", xhr.status);
            assert(xhr.status === 200);
            throw new RequestStatusException(999);
        }
    )
        .proxy(contract.acceptor.echoService)
        .echo("wrongStatus")
        .catch(error => log("wrongStatus failed:", error));

    new yass.XhrClient("http://" + hostname + ":9999/xhr", messageSerializer)
        .proxy(contract.acceptor.echoService)
        .echo("serverDown")
        .catch(error => log("serverDown failed:", error));

    log("xhrTest done");

})();

log("all done");
