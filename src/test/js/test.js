'use strict';

var yass;

function assert(value) {
  if (!value) {
    throw new Error("assert failed");
  }
}

function exception(action) {
  var thrown = false;
  try {
    action();
  } catch (e) {
    thrown = true;
    console.log("expected error caught:", e);
  }
  assert(thrown);
}

function writer2reader(writer) {
  var byteArray = writer.getUint8Array();
  var arrayBuffer = new ArrayBuffer(byteArray.length);
  new Uint8Array(arrayBuffer).set(byteArray);
  return yass.reader(arrayBuffer);
}

//----------------------------------------------------------------------------------------------------------------------
// Reader/Writer

(function () {

  var writer = yass.writer(1);
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
  assert(writer.getUint8Array().length === 74);

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
  exception(function () {
    reader.readByte();
  });

  writer = yass.writer(100);
  writer.writeByte(128);
  writer.writeByte(128);
  writer.writeByte(128);
  writer.writeByte(128);
  writer.writeByte(128);
  reader = writer2reader(writer);
  exception(function () {
    reader.readVarInt();
  });

  function utf8(bytes, value) {
    var writer = yass.writer(100);
    writer.writeUtf8(value);
    assert(writer.getUint8Array().length === bytes);
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

}());

//----------------------------------------------------------------------------------------------------------------------
// Enum

(function () {
  var ask = contract.PriceType.ASK;
  console.log(ask);
  assert(ask instanceof contract.PriceType);
  assert(ask instanceof yass.Enum);
  assert(!(ask instanceof yass.Class));
  assert(ask.value === 1);
  assert(ask.name === "ASK");
  assert(ask === contract.PriceType.ASK);
  assert(ask !== contract.PriceType.BID);
}());

//----------------------------------------------------------------------------------------------------------------------
// Class

(function () {
  var stock = new contract.instrument.Stock();
  stock.id = "1344";
  stock.name = "IBM";
  stock.paysDividend = true;
  console.log(stock);
  assert(stock instanceof yass.Class);
  assert(stock instanceof contract.Instrument);
  assert(stock instanceof contract.instrument.Stock);
  assert(!(stock instanceof contract.instrument.Bond));
}());

//----------------------------------------------------------------------------------------------------------------------
// Serializer

(function () {

  function copy(value) {
    var writer = yass.writer(100);
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

  function compare(array1, array2) {
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

  var stock = new contract.instrument.Stock();
  stock.id = "1344";
  stock.name = "IBM";
  stock.paysDividend = true;
  stock = copy(stock);
  assert(stock.id === "1344");
  assert(stock.name === "IBM");
  assert(stock.paysDividend);
  stock.paysDividend = false;
  stock = copy(stock);
  assert(!stock.paysDividend);
  stock.paysDividend = null;
  stock = copy(stock);
  assert(stock.paysDividend === null);

  var bond = new contract.instrument.Bond();
  bond.coupon = 1234;
  bond = copy(bond);
  assert(bond.coupon === 1234);

  var e = new contract.UnknownInstrumentsException();
  e.instrumentIds = ["a", "b"];
  e.comment = bond;
  e = copy(e);
  assert(compare(e.instrumentIds , ["a", "b"]));
  assert(e.comment.coupon === 1234);

}());

//----------------------------------------------------------------------------------------------------------------------
// Interceptor

(function () {

  function greeter(method, parameters, proceed) {
    console.log("hello");
    try {
      var result = proceed();
      console.log("see you again");
      return result;
    } catch (e) {
      console.log("don't come back");
      throw e;
    }
  }

  function logger(method, parameters, proceed) {
    console.log("entry:", method, parameters);
    try {
      var result = proceed();
      console.log("exit:", method, result);
      return result;
    } catch (e) {
      console.log("exception:", method, e);
      throw e;
    }
  }

  assert(
    yass.direct(null, null, function () {
      return 123;
    }) === 123
  );

  assert(yass.composite(yass.direct, logger) === logger);
  assert(yass.composite(logger, yass.direct) === logger);

  var greeterLogger = yass.composite(greeter, logger);

  assert(
    greeterLogger(null, null, function () {
      return 123;
    }) === 123
  );

  exception(function () {
    greeterLogger(null, null, function () {
      throw new Error("greeterLogger");
    });
  });

}());

//----------------------------------------------------------------------------------------------------------------------
// Client/Server

(function () {

  var client = function (server) {
    return yass.create(yass.client(), {
      invoke: function (clientInvocation) {
        return clientInvocation.invoke(function (request, replyCallback) {
          yass.processReply(server.invocation(request).invoke(), replyCallback);
        });
      }
    });
  };

  var throwException = false;

  var instrumentServiceImpl = yass.create(contract.InstrumentService, {
    reload: function (testBoolean, testInt) {
      console.log("reload:", testBoolean, testInt);
    },
    getInstruments: function () {
      if (throwException) {
        throw new Error("error in getInstruments");
      }
      var stock = new contract.instrument.Stock();
      stock.paysDividend = true;
      stock.name = "IBM";
      return [stock, stock];
    }
  });

  var priceEngineImpl = yass.create(contract.PriceEngine, {
    subscribe: function (instrumentIds) {
      console.log("subscribe:", instrumentIds);
    }
  });

  function logger(type) {
    return function (method, parameters, proceed) {
      function log(kind, data) {
        console.log("logger:", type, kind, method, data);
      }
      log("entry", parameters);
      try {
        var result = proceed();
        log("exit", result);
        return result;
      } catch (e) {
        log("exception", e);
        throw e;
      }
    };
  }

  var server = yass.server([
    contract.ServerServices.InstrumentService.service(instrumentServiceImpl, logger("server")),
    contract.ServerServices.PriceEngine.service(priceEngineImpl, logger("server"))
  ]);

  var session = client(server);

  var instrumentService = contract.ServerServices.InstrumentService.invoker(session)(logger("client"));
  var priceEngine = contract.ServerServices.PriceEngine.invoker(session)(logger("client"));

  priceEngine.subscribe(["945", "4883"], function (reply) {
    // $$$ note: reply must always be called even if no return type to get exceptions
    console.log("callback subscribe", reply());
  });

  instrumentService.reload(true, 987654);

  instrumentService.getInstruments(function (reply) {
    console.log("callback getInstruments", reply());
  });

  throwException = true;
  exception(function () {
    instrumentService.getInstruments(function (reply) {
      reply(); // $$$ throws exception
    });
  });
  throwException = false;

  function localConnection(setup1, setup2) {
    function connection() {
      return {
        other: null,
        write: function (packet) {
          this.other.received(packet);
        },
        closed: function () {
          this.other.close();
        }
      };
    }
    var connection1 = connection();
    var connection2 = connection();
    connection2.other = setup1.createSession(connection1);
    try {
      connection1.other = setup2.createSession(connection2);
    } catch (exception) {
      connection2.other.close(exception);
      throw exception;
    }
    if (!(connection1.other.open() && connection2.other.open())) {
      throw Error("open failed");
    }
  }

  localConnection(
    yass.sessionSetup(
      yass.server([
        contract.ClientServices.PriceListener.service(
          yass.create(contract.PriceListener, {
            newPrices: function (prices) {
              console.log("newPrices:", prices);
            }
          }),
          logger("client")
        )
      ]),
      function (setup, connection) {
        return yass.create(yass.session(setup, connection), {
          closed: function (exception) {
            console.log("client closed", exception);
          },
          opened: function () {
            console.log("client opened");
            var instrumentService = contract.ServerServices.InstrumentService.invoker(this)(logger("client"));
            var priceEngine = contract.ServerServices.PriceEngine.invoker(this)(logger("client"));
            instrumentService.reload(true, 987654);
            instrumentService.getInstruments(function (reply) {
              console.log("callback getInstruments", reply()); // $$$ does not work, because we don't have threads here
            });
            this.close();
          }
        });
      }
    ),
    yass.sessionSetup(
      server,
      function (setup, connection) {
        return yass.create(yass.session(setup, connection), {
          closed: function (exception) {
            console.log("server closed", exception);
          },
          opened: function () {
            console.log("server opened");
          }
        });
      }
    )
  );

}());

//----------------------------------------------------------------------------------------------------------------------

console.log("done");
