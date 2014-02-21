'use strict';

//----------------------------------------------------------------------------------------------------------------------
// Writer

function printWriter(writer) {
  console.log(writer.bytes());
}

var writer = new yass.Writer(1);
printWriter(writer);

writer.writeByte(123);
printWriter(writer);
writer.writeByte(210);
printWriter(writer);

writer.writeInt(1);
printWriter(writer);
writer.writeInt(257);
printWriter(writer);
writer.writeInt(-1);
printWriter(writer);

writer.writeVarInt(1);
printWriter(writer);
writer.writeVarInt(127);
printWriter(writer);
writer.writeVarInt(128);
printWriter(writer);
writer.writeVarInt(129);
printWriter(writer);
writer.writeVarInt(-1);
printWriter(writer);

writer.writeZigZagInt(1);
printWriter(writer);
writer.writeZigZagInt(-1);
printWriter(writer);

//----------------------------------------------------------------------------------------------------------------------
// Reader

var reader = new yass.Reader(writer.bytes().buffer);

console.log(reader.readByte());
console.log(reader.readByte());

console.log(reader.readInt());
console.log(reader.readInt());
console.log(reader.readInt());

console.log(reader.readVarInt());
console.log(reader.readVarInt());
console.log(reader.readVarInt());
console.log(reader.readVarInt());
console.log(reader.readVarInt());

console.log(reader.readZigZagInt());
console.log(reader.readZigZagInt());

try {
  reader.readByte();
} catch(exception) {
  console.log(exception);
}

//----------------------------------------------------------------------------------------------------------------------
