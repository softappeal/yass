// $todo: review

'use strict';

var yass = {};

yass.inherits = function (child, parent) {
  child.prototype = Object.create(parent.prototype);
  child.prototype.constructor = child;
};

yass.service = function (id, implementation /* , interceptors... */) {
};

yass.proxy = function (session, id /* , interceptors... */) {
};

yass.rpc = function (result, callback) {
  callback(result);
};

//----------------------------------------------------------------------------------------------------------------------
// Writer

yass.Writer = function (initialCapacity) {
  this.capacity = initialCapacity;
  this.position = 0;
  this.array = new Uint8Array(initialCapacity);
};

yass.Writer.prototype.getUint8Array = function () {
  return this.array.subarray(0, this.position);
};

yass.Writer.prototype.needed = function (value) {
  var oldPosition = this.position;
  var oldArray;
  this.position += value;
  if (this.position > this.capacity) {
    oldArray = this.array;
    this.capacity = 2 * this.position;
    this.array = new Uint8Array(this.capacity);
    this.array.set(oldArray);
  }
  return oldPosition;
};

yass.Writer.prototype.writeByte = function (value) {
  var position = this.needed(1);
  this.array[position] = value;
};

yass.Writer.prototype.writeInt = function (value) {
  var position = this.needed(4);
  new DataView(this.array.buffer).setInt32(position, value);
};

yass.Writer.prototype.writeVarInt = function (value) {
  while (true) {
    if ((value & ~0x7F) === 0) {
      this.writeByte(value);
      return;
    }
    this.writeByte((value & 0x7F) | 0x80);
    value >>>= 7;
  }
};

yass.Writer.prototype.writeZigZagInt = function (value) {
  this.writeVarInt((value << 1) ^ (value >> 31));
};

//----------------------------------------------------------------------------------------------------------------------
// Reader

yass.Reader = function (arrayBuffer) {
  this.array = new Uint8Array(arrayBuffer);
  this.length = arrayBuffer.byteLength;
  this.position = 0;
};

yass.Reader.prototype.isEmpty = function () {
  return this.position >= this.length;
};

yass.Reader.prototype.needed = function (value) {
  var oldPosition = this.position;
  this.position += value;
  if (this.position > this.length) {
    throw "reader buffer underflow";
  }
  return oldPosition;
};

yass.Reader.prototype.readByte = function () {
  return this.array[this.needed(1)];
};

yass.Reader.prototype.readInt = function () {
  return new DataView(this.array.buffer).getInt32(this.needed(4));
};

yass.Reader.prototype.readVarInt = function () {
  var shift = 0;
  var value = 0;
  var b;
  while (shift < 32) {
    b = this.readByte();
    value |= (b & 0x7F) << shift;
    if ((b & 0x80) === 0) {
      return value;
    }
    shift += 7;
  }
  throw "malformed input";
};

yass.Reader.prototype.readZigZagInt = function () {
  var value = this.readVarInt();
  return (value >>> 1) ^ -(value & 1);
};

//----------------------------------------------------------------------------------------------------------------------
// TypeHandler

yass.TypeHandler = function () {
  // empty
};

yass.TypeHandler.prototype.read = function (input) {
  throw "abstract method";
};

yass.TypeHandler.prototype.write = function (value, output) {
  throw "abstract method";
};

yass.TypeHandler.prototype.writeId = function (id, value, output) {
  output.writer.writeVarInt(id);
  this.write(value, output);
};

//----------------------------------------------------------------------------------------------------------------------
// NULL

yass.NULL = new yass.TypeHandler();

yass.NULL.read = function (input) {
  return null;
};

yass.NULL.write = function (value, output) {
  // empty
};

//----------------------------------------------------------------------------------------------------------------------
// LIST

yass.LIST = new yass.TypeHandler();

yass.LIST.read = function (input) {
  /* $todo
  var length = input.reader.readVarInt();
  var list = new ArrayList < > (Math.min(length, 256)); // note: prevents out-of-memory attack
  while (length-- > 0) {
    list.add(input.read());
  }
  return list;
  */
};

yass.LIST.write = function (value, output) {
  /* $todo
  var list = value;
  output.writer.writeVarInt(list.size());
  for (e : list ) {
    output.write(e);
  }
  */
};

//----------------------------------------------------------------------------------------------------------------------
// Output

yass.Output = function (writer, meta) {
  this.writer = writer;
};

yass.Output.prototype.write = function (value) {
  /* $todo
  var typeDesc;
  if (value === null) {
    yass.NULL.writeId(0, null, this);
  } else if (value instanceof List) {
    yass.LIST.writeId(2, value, this);
  } else {
    // boolean, string, number, rest, $todo enum pattern is not yet good enough! -> $todo get TypeHandler from MetaData
    typeDesc = this.class2typeDesc.get(value.getClass());
    if (typeDesc === null) {
      throw "missing type '" + value.getClass().getCanonicalName() + '\'';
    }
    typeDesc.writeId(id, value, this);
  }
  */
};

//----------------------------------------------------------------------------------------------------------------------
// Input

yass.Input = function (reader, id2typeHandler) {
  this.reader = reader;
  this.id2typeHandler = id2typeHandler;
};

yass.Input.prototype.read = function () {
  return this.id2typeHandler[this.reader.readVarInt()].read(this);
};

//----------------------------------------------------------------------------------------------------------------------
// BaseTypeHandler

yass.BaseTypeHandler = function () {
  yass.TypeHandler.call(this);
};
yass.inherits(yass.BaseTypeHandler, yass.TypeHandler);

yass.BaseTypeHandler.prototype.read = function (reader) {
  throw "abstract method";
};

yass.BaseTypeHandler.prototype.write = function (value, writer) {
  throw "abstract method";
};

yass.BaseTypeHandler.prototype.read = function (input) {
  return this.read(input.reader);
};

yass.BaseTypeHandler.prototype.write = function (value, output) {
  this.write(value, output.writer);
};

//----------------------------------------------------------------------------------------------------------------------
// BOOLEAN

yass.BOOLEAN = new yass.BaseTypeHandler();

yass.BOOLEAN.read = function (reader) {
  return reader.readByte() !== 0;
};

yass.BOOLEAN.write = function (value, writer) {
  writer.writeByte(value ? 1 : 0);
};

//----------------------------------------------------------------------------------------------------------------------
// INTEGER

yass.INTEGER = new yass.BaseTypeHandler();

yass.INTEGER.read = function (reader) {
  return reader.readZigZagInt();
};

yass.INTEGER.write = function (value, writer) {
  writer.writeZigZagInt(value);
};

//----------------------------------------------------------------------------------------------------------------------
// BYTE_ARRAY

yass.BYTE_ARRAY = new yass.BaseTypeHandler();

yass.BYTE_ARRAY.read = function (reader) {
  /* $todo
  length = reader.readVarInt();
  value = new byte[Math.min(length, 1024)];
  for (i = 0; i < length; i++) {
    if (i >= value.length) {
      value = Arrays.copyOf(value, Math.min(length, 2 * value.length)); // note: prevents out-of-memory attack
    }
    value[i] = reader.readByte();
  }
  return value;
  */
};

yass.BYTE_ARRAY.write = function (value, writer) {
  /* $todo
   writer.writeVarInt(value.length);
   writer.writeBytes(value);
   */
};

//----------------------------------------------------------------------------------------------------------------------
// STRING

yass.STRING = new yass.BaseTypeHandler();

yass.STRING.read = function (reader) {
  // $todo return Utf8.string(BYTE_ARRAY.read(reader));
};

yass.STRING.write = function (value, writer) {
  // $todo BYTE_ARRAY.write(Utf8.bytes(value), writer);
};

//----------------------------------------------------------------------------------------------------------------------
// FieldHandler

yass.FieldHandler = function (field, typeHandler) {
  this.field = field;
  this.typeHandler = typeHandler;
};

yass.FieldHandler.prototype.read = function (object, input) {
  object[this.field] = (this.typeHandler === null) ? input.read() : this.typeHandler.read(input);
};

yass.FieldHandler.prototype.write = function (id, object, output) {
  var value = object[this.field];
  if (value !== null) {
    output.writer.writeVarInt(id);
    if (this.typeHandler === null) {
      output.write(value);
    } else {
      this.typeHandler.write(value, output);
    }
  }
};

//----------------------------------------------------------------------------------------------------------------------
// ClassTypeHandler

yass.ClassTypeHandler = function (creator, fieldId2handler) {
  yass.TypeHandler.call(this);
  this.creator = creator;
  this.fieldId2handler = fieldId2handler;
};
yass.inherits(yass.ClassTypeHandler, yass.TypeHandler);

yass.ClassTypeHandler.prototype.read = function (input) {
  var object = this.creator();
  var id;
  while (true) {
    id = input.reader.readVarInt();
    if (id === 0) {
      return object;
    }
    this.fieldId2handler[id].read(object, input);
  }
};

yass.ClassTypeHandler.prototype.write = function (value, output) {
  this.fieldId2handler.forEach(function (handler, id) {
    handler.write(id, value, output);
  });
  output.writer.writeVarInt(0);
};

//----------------------------------------------------------------------------------------------------------------------
// Serializer

yass.Serializer = function (meta) {
  this.meta = meta;
};

yass.Serializer.prototype.read = function (reader) {
  return new yass.Input(reader, this.meta).read();
};

yass.Serializer.prototype.writer = function (value, writer) {
  new yass.Output(writer, this.meta).write(value);
};

//----------------------------------------------------------------------------------------------------------------------
