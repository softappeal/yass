// $todo: review

'use strict';

var yass = {};

yass.inherits = function (child, parent) {
  child.prototype = Object.create(parent.prototype);
  child.prototype.constructor = child;
};

//----------------------------------------------------------------------------------------------------------------------
// Writer

yass.writer = function (initialCapacity) {

  var capacity = initialCapacity;
  var position = 0;
  var array = new Uint8Array(initialCapacity);

  function needed(value) {
    var oldPosition = position;
    position += value;
    if (position > capacity) {
      var oldArray = array;
      capacity = 2 * position;
      array = new Uint8Array(capacity);
      array.set(oldArray);
    }
    return oldPosition;
  }

  return {

    writeByte: function (value) {
      var position = needed(1);
      array[position] = value;
    },

    writeInt: function (value) {
      var position = needed(4);
      new DataView(array.buffer).setInt32(position, value);
    },

    writeVarInt: function (value) {
      while (true) {
        if ((value & ~0x7F) === 0) {
          this.writeByte(value);
          return;
        }
        this.writeByte((value & 0x7F) | 0x80);
        value >>>= 7;
      }
    },

    writeZigZagInt: function (value) {
      this.writeVarInt((value << 1) ^ (value >> 31));
    },

    writeUtf8: function (value) {
      for (var c = 0; c < value.length; c++) {
        var code = value.charCodeAt(c);
        if (code < 0x80) { // 0xxx xxxx
          this.writeByte(code);
        } else if (code < 0x800) { // 110x xxxx  10xx xxxx
          this.writeByte(0xC0 | ((code >> 6) & 0x1F));
          this.writeByte(0x80 | (code & 0x3F));
        } else { // 1110 xxxx  10xx xxxx  10xx xxxx
          this.writeByte(0xE0 | ((code >> 12) & 0x0F));
          this.writeByte(0x80 | ((code >> 6) & 0x3F));
          this.writeByte(0x80 | (code & 0x3F));
        }
      }
    },

    getUint8Array: function () {
      return array.subarray(0, position);
    }

  };
};

//----------------------------------------------------------------------------------------------------------------------
// Reader

yass.reader = function (arrayBuffer) {

  var array = new Uint8Array(arrayBuffer);
  var length = arrayBuffer.byteLength;
  var position = 0;

  function needed(value) {
    var oldPosition = position;
    position += value;
    if (position > length) {
      throw new Error("reader buffer underflow");
    }
    return oldPosition;
  }

  return {

    isEmpty: function () {
      return position >= length;
    },

    readByte: function () {
      return array[needed(1)];
    },

    readInt: function () {
      return new DataView(array.buffer).getInt32(needed(4));
    },

    readVarInt: function () {
      var shift = 0;
      var value = 0;
      while (shift < 32) {
        var b = this.readByte();
        value |= (b & 0x7F) << shift;
        if ((b & 0x80) === 0) {
          return value;
        }
        shift += 7;
      }
      throw new Error("malformed VarInt input");
    },

    readZigZagInt: function () {
      var value = this.readVarInt();
      return (value >>> 1) ^ -(value & 1);
    },

    readUtf8: function (bytes) {
      var result = "";
      while (bytes-- > 0) {
        var code;
        var b1 = this.readByte();
        if ((b1 & 0x80) === 0) { // 0xxx xxxx
          code = b1;
        } else if ((b1 & 0xE0) === 0xC0) { // 110x xxxx  10xx xxxx
          var b2 = this.readByte();
          if ((b2 & 0xC0) !== 0x80) {
            throw new Error("malformed String input (1)");
          }
          code = ((b1 & 0x1F) << 6) | (b2 & 0x3F);
          bytes--;
        } else if ((b1 & 0xF0) === 0xE0) { // 1110 xxxx  10xx xxxx  10xx xxxx
          var b2 = this.readByte();
          var b3 = this.readByte();
          if (((b2 & 0xC0) !== 0x80) || ((b3 & 0xC0) !== 0x80)) {
            throw new Error("malformed String input (2)");
          }
          code = ((b1 & 0x0F) << 12) | ((b2 & 0x3F) << 6) | (b3 & 0x3F);
          bytes -= 2;
        } else {
          throw new Error("malformed String input (3)");
        }
        result += String.fromCharCode(code);
      }
      return result;
    }

  };
};

//----------------------------------------------------------------------------------------------------------------------
// Class

yass.Class = function () {
  // empty
};

//----------------------------------------------------------------------------------------------------------------------
// Enum

yass.Enum = function (value, name) {
  this.value = value;
  this.name = name;
};

//----------------------------------------------------------------------------------------------------------------------
// TypeDesc

yass.typeDesc = function (id, handler) {
  return {
    id: id,
    handler: handler,
    write: function (value, output) {
      output.writer.writeVarInt(id);
      handler.write(value, output);
    }
  };
};

//----------------------------------------------------------------------------------------------------------------------
// TypeHandler

// $$$ start review here

yass.TypeHandler = function () {
  // empty
};

yass.TypeHandler.prototype.read = function (input) {
  throw new Error("abstract method called");
};

yass.TypeHandler.prototype.write = function (value, output) {
  throw new Error("abstract method called");
};

//----------------------------------------------------------------------------------------------------------------------
// BaseTypeHandler

yass.BaseTypeHandler = function () {
  yass.TypeHandler.call(this);
};
yass.inherits(yass.BaseTypeHandler, yass.TypeHandler);

yass.BaseTypeHandler.prototype.readBase = function (reader) {
  throw new Error("abstract method called");
};

yass.BaseTypeHandler.prototype.writeBase = function (value, writer) {
  throw new Error("abstract method called");
};

yass.BaseTypeHandler.prototype.read = function (input) {
  return this.readBase(input.reader);
};

yass.BaseTypeHandler.prototype.write = function (value, output) {
  this.writeBase(value, output.writer);
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

yass.NULL.TYPE_DESC = yass.typeDesc(0, yass.NULL);

//----------------------------------------------------------------------------------------------------------------------
// LIST

yass.LIST = new yass.TypeHandler();

yass.LIST.read = function (input) {
  var list = [];
  for (var size = input.reader.readVarInt(); size > 0; size--) {
    list.push(input.read());
  }
  return list;
};

yass.LIST.write = function (value, output) {
  output.writer.writeVarInt(value.length)
  value.forEach(function (value) {
    output.write(value);
  });
};

yass.LIST.TYPE_DESC = yass.typeDesc(2, yass.LIST);

//----------------------------------------------------------------------------------------------------------------------
// BOOLEAN

yass.BOOLEAN = new yass.BaseTypeHandler();

yass.BOOLEAN.readBase = function (reader) {
  return reader.readByte() !== 0;
};

yass.BOOLEAN.writeBase = function (value, writer) {
  writer.writeByte(value ? 1 : 0);
};

yass.BOOLEAN.TYPE_DESC = yass.typeDesc(3, yass.BOOLEAN);

//----------------------------------------------------------------------------------------------------------------------
// INTEGER

yass.INTEGER = new yass.BaseTypeHandler();

yass.INTEGER.readBase = function (reader) {
  return reader.readZigZagInt();
};

yass.INTEGER.writeBase = function (value, writer) {
  writer.writeZigZagInt(value);
};

yass.INTEGER.TYPE_DESC = yass.typeDesc(4, yass.INTEGER);

//----------------------------------------------------------------------------------------------------------------------
// STRING

yass.STRING = new yass.BaseTypeHandler();

yass.STRING.readBase = function (reader) {
  return reader.readUtf8(reader.readVarInt());
};

yass.STRING.writeBase = function (value, writer) {
  function calcUtf8bytes(value) {
    var bytes = 0;
    for (var c = 0; c < value.length; c++) {
      var code = value.charCodeAt(c);
      if (code < 0x80) {
        bytes += 1;
      } else if (code < 0x800) {
        bytes += 2;
      } else {
        bytes += 3;
      }
    }
    return bytes;
  }
  writer.writeVarInt(calcUtf8bytes(value));
  writer.writeUtf8(value);
};

yass.STRING.TYPE_DESC = yass.typeDesc(5, yass.STRING);

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
// Output

yass.Output = function (writer) {
  this.writer = writer;
};

yass.Output.prototype.write = function (value) {
  if (value === null) {
    yass.NULL.TYPE_DESC.write(null, this);
  } else if (typeof value === "boolean") {
    yass.BOOLEAN.TYPE_DESC.write(value, this);
  } else if (typeof value === "number") {
    yass.INTEGER.TYPE_DESC.write(value, this);
  } else if (typeof value === "string") {
    yass.STRING.TYPE_DESC.write(value, this);
  } else if (Array.isArray(value)) {
    yass.LIST.TYPE_DESC.write(value, this);
  } else if (value instanceof yass.Enum) {
    value.constructor.TYPE_DESC.write(value, this);
  } else if (value instanceof yass.Class) {
    value.constructor.TYPE_DESC.write(value, this);
  } else {
    throw new Error("unexpected value type");
  }
};

//----------------------------------------------------------------------------------------------------------------------
// EnumTypeHandler

yass.EnumTypeHandler = function (values) {
  yass.BaseTypeHandler.call(this);
  this.values = values;
};
yass.inherits(yass.EnumTypeHandler, yass.BaseTypeHandler);

yass.EnumTypeHandler.prototype.readBase = function (reader) {
  return this.values[reader.readVarInt()];
};

yass.EnumTypeHandler.prototype.writeBase = function (value, writer) {
  writer.writeVarInt(value.value);
};

yass.enumConstructor = function () {
  var type = function (value, name) {
    yass.Enum.call(this, value, name);
  };
  yass.inherits(type, yass.Enum);
  return type;
};

yass.enumDesc = function (id, constructor) {
  var values = [];
  Object.keys(constructor).forEach(function (property) {
    var constant = constructor[property];
    if (constant instanceof yass.Enum) {
      values[constant.value] = constant;
    }
  });
  constructor.TYPE_DESC = yass.typeDesc(id, new yass.EnumTypeHandler(values));
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

yass.ClassTypeHandler = function (constructor) {
  yass.TypeHandler.call(this);
  this.constructor = constructor;
  this.fieldId2handler = [];
};
yass.inherits(yass.ClassTypeHandler, yass.TypeHandler);

yass.ClassTypeHandler.prototype.read = function (input) {
  var object = new this.constructor();
  while (true) {
    var id = input.reader.readVarInt();
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

yass.classDesc = function (id, constructor) {
  constructor.TYPE_DESC = yass.typeDesc(id, new yass.ClassTypeHandler(constructor));
};

yass.classField = function (constructor, id, name, typeDescOwner) {
  constructor.TYPE_DESC.handler.fieldId2handler[id] = new yass.FieldHandler(
    name, (typeDescOwner === null) ? null : typeDescOwner.TYPE_DESC.handler
  );
};

//----------------------------------------------------------------------------------------------------------------------
// Serializer

yass.Serializer = function (root) {
  this.id2typeHandler = [];
  var that = this;
  function addHandler(typeDescOwner) {
    var typeDesc = typeDescOwner.TYPE_DESC;
    that.id2typeHandler[typeDesc.id] = typeDesc.handler;
  }
  addHandler(yass.NULL);
  addHandler(yass.LIST);
  addHandler(yass.BOOLEAN);
  addHandler(yass.INTEGER);
  addHandler(yass.STRING);
  function addPackage(root) {
    for (var name in root) {
      if (!root.hasOwnProperty(name)) {
        continue;
      }
      var property = root[name];
      if (property.hasOwnProperty("TYPE_DESC")) {
        addHandler(property);
      } else {
        addPackage(property);
      }
    }
  }
  addPackage(root);
};

yass.Serializer.prototype.read = function (reader) {
  return new yass.Input(reader, this.id2typeHandler).read();
};

yass.Serializer.prototype.write = function (value, writer) {
  new yass.Output(writer).write(value);
};

//----------------------------------------------------------------------------------------------------------------------

// $todo

yass.service = function (id, implementation /* , interceptors... */) {
};

yass.proxy = function (session, id /* , interceptors... */) {
};

yass.rpc = function (result, callback) {
  callback(result);
};

//----------------------------------------------------------------------------------------------------------------------
