'use strict';

var yass = {};

yass.inherits = function (child, parent) {
  child.prototype = Object.create(parent.prototype);
  child.prototype.constructor = child;
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
