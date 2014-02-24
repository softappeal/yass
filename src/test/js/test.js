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

var Color = function (value, name) {
  yass.Enum.call(this, value, name);
};
yass.inherits(Color, yass.Enum);
Color.RED = new Color(0, "RED");
Color.BLUE = new Color(2, "BLUE");
Color.TYPE_DESC = 123;

(function () {
  var values = yass.Enum.values(Color);
  var red = Color.RED;
  console.log(red);
  assert(red instanceof Color);
  assert(red instanceof yass.Enum);
  assert(!(red instanceof yass.TypeHandler));
  assert(red.value === 0);
  assert(red.name === "RED");
  assert(red === Color.RED);
  assert(red !== Color.BLUE);
  assert(red.constructor.TYPE_DESC === 123);
  assert(values.length === 3);
  assert(values[0] === Color.RED);
  assert(values[1] === undefined);
  assert(values[2] === Color.BLUE);
})();

//----------------------------------------------------------------------------------------------------------------------
// Class

var Instrument = function () {
  this.id = null;
  this.name = null;
};
yass.inherits(Instrument, yass.Class);
Instrument.TYPE_DESC = 30;

var Stock = function () {
  Instrument.call(this);
  this.paysDividend = null;
};
yass.inherits(Stock, Instrument);
Stock.TYPE_DESC = 31;

(function () {
  assert(Instrument.TYPE_DESC === 30);
  assert(Stock.TYPE_DESC === 31);
  var stock = new Stock();
  stock.id = 123;
  stock.name = "IBM";
  stock.paysDividend = true;
  console.log(stock);
  assert(stock.constructor.TYPE_DESC === 31);
  assert(stock instanceof yass.Class);
  assert(stock instanceof Instrument);
  assert(stock instanceof Stock);
  assert(!(stock instanceof yass.TypeHandler));
})();

//----------------------------------------------------------------------------------------------------------------------
