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

//----------------------------------------------------------------------------------------------------------------------
// Reader/Writer

(function () {

  var arrayBuffer;
  var byteArray;
  var reader;

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

  byteArray = writer.getUint8Array();
  assert(byteArray.byteLength === 74);
  arrayBuffer = new ArrayBuffer(byteArray.length);
  new Uint8Array(arrayBuffer).set(byteArray);

  reader = new yass.Reader(arrayBuffer);
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

  writer = new yass.Writer(100);
  writer.writeByte(128);
  writer.writeByte(128);
  writer.writeByte(128);
  writer.writeByte(128);
  writer.writeByte(128);
  byteArray = writer.getUint8Array();
  arrayBuffer = new ArrayBuffer(byteArray.length);
  new Uint8Array(arrayBuffer).set(byteArray);
  reader = new yass.Reader(arrayBuffer);
  exception(function () {
    reader.readVarInt();
  });

  function utf8(bytes, value) {
    assert(yass.Writer.calcUtf8bytes(value) === bytes);
    writer = new yass.Writer(100);
    writer.writeUtf8(value);
    byteArray = writer.getUint8Array();
    assert(byteArray.length === bytes);
    arrayBuffer = new ArrayBuffer(byteArray.length);
    new Uint8Array(arrayBuffer).set(byteArray);
    reader = new yass.Reader(arrayBuffer);
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

//----------------------------------------------------------------------------------------------------------------------
// Enum

(function () {
  var ask = contract.PriceType.ASK;
  var values = ask.constructor.TYPE_DESC.handler.values;
  console.log(ask);
  assert(ask instanceof contract.PriceType);
  assert(ask instanceof yass.Enum);
  assert(!(ask instanceof yass.TypeHandler));
  assert(ask.value === 1);
  assert(ask.name === "ASK");
  assert(ask === contract.PriceType.ASK);
  assert(ask !== contract.PriceType.BID);
  assert(ask.constructor.TYPE_DESC.id === 6);
  assert(values.length === 2);
  assert(values[0] === contract.PriceType.BID);
  assert(values[1] === contract.PriceType.ASK);

})();

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
  assert(stock.constructor.TYPE_DESC.id === 8);
})();

//----------------------------------------------------------------------------------------------------------------------
// Serializer

(function () {
  function copy(value) {
    var writer = new yass.Writer(100);
    contract.SERIALIZER.write(value, writer);
    var byteArray = writer.getUint8Array();
    var arrayBuffer = new ArrayBuffer(byteArray.length);
    new Uint8Array(arrayBuffer).set(byteArray);
    var reader = new yass.Reader(arrayBuffer);
    var result = contract.SERIALIZER.read(reader);
    assert(reader.isEmpty());
    return result;
  }

  var stock = new contract.instrument.Stock();
  stock.id = "1344";
  stock.name = "IBM";
  stock.paysDividend = true;
  console.log(copy(stock));
})();

//----------------------------------------------------------------------------------------------------------------------
