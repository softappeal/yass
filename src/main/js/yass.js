// $todo: review

var yass = (function () {
  'use strict';

  function create(proto, props) { // $todo: is there a better way ?
    var o = Object.create(proto);
    for (var p in props) {
      if (props.hasOwnProperty(p)) {
        o[p] = props[p];
      }
    }
    return o;
  }

  function inherits(child, parent) { // $todo: is this ok ?
    child.prototype = Object.create(parent.prototype);
    child.prototype.constructor = child;
  }

  function writer(initialCapacity) {
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
  }

  function reader(arrayBuffer) {
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
  }

  function Class() { // baseclass for class types
    // empty
  }

  function Enum(value, name) { // baseclass for enum types
    this.value = value;
    this.name = name;
  }

  function typeDesc(id, handler) {
    return {
      id: id,
      handler: handler,
      write: function (value, output) {
        output.writer.writeVarInt(id);
        handler.write(value, output);
      }
    };
  }

  var typeHandler = {
    read: function (input) {
      throw new Error("abstract method called");
    },
    write: function (value, output) {
      throw new Error("abstract method called");
    }
  };

  var baseTypeHandler = create(typeHandler, {
    readBase: function (reader) {
      throw new Error("abstract method called");
    },
    writeBase: function (value, writer) {
      throw new Error("abstract method called");
    },
    read: function (input) {
      return this.readBase(input.reader);
    },
    write: function (value, output) {
      this.writeBase(value, output.writer);
    }
  });

  function typeDescOwner(id, proto, props) {
    var o = create(proto, props);
    o.TYPE_DESC = typeDesc(id, o);
    return o;
  }

  var NULL = typeDescOwner(0, typeHandler, {
    read: function (input) {
      return null;
    },
    write: function (value, output) {
      // empty
    }
  });

  var LIST = typeDescOwner(2, typeHandler, {
    read: function (input) {
      var list = [];
      for (var size = input.reader.readVarInt(); size > 0; size--) {
        list.push(input.read());
      }
      return list;
    },
    write: function (value, output) {
      output.writer.writeVarInt(value.length);
      value.forEach(function (value) {
        output.write(value);
      });
    }
  });

  var BOOLEAN = typeDescOwner(3, baseTypeHandler, {
    readBase: function (reader) {
      return reader.readByte() !== 0;
    },
    writeBase: function (value, writer) {
      writer.writeByte(value ? 1 : 0);
    }
  });

  var INTEGER = typeDescOwner(4, baseTypeHandler, {
    readBase: function (reader) {
      return reader.readZigZagInt();
    },
    writeBase: function (value, writer) {
      writer.writeZigZagInt(value);
    }
  });

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

  var STRING = typeDescOwner(5, baseTypeHandler, {
    readBase: function (reader) {
      return reader.readUtf8(reader.readVarInt());
    },
    writeBase: function (value, writer) {
      writer.writeVarInt(calcUtf8bytes(value));
      writer.writeUtf8(value);
    }
  });

  function input(reader, id2typeHandler) {
    return {
      reader: reader,
      read: function () {
        return id2typeHandler[reader.readVarInt()].read(this);
      }
    };
  }

  function output(writer) {
    return {
      writer: writer,
      write: function (value) {
        if (value === null) {
          NULL.TYPE_DESC.write(null, this);
        } else if (typeof value === "boolean") {
          BOOLEAN.TYPE_DESC.write(value, this);
        } else if (typeof value === "number") {
          INTEGER.TYPE_DESC.write(value, this);
        } else if (typeof value === "string") {
          STRING.TYPE_DESC.write(value, this);
        } else if (Array.isArray(value)) {
          LIST.TYPE_DESC.write(value, this);
        } else if (value instanceof Enum) {
          value.constructor.TYPE_DESC.write(value, this);
        } else if (value instanceof Class) {
          value.constructor.TYPE_DESC.write(value, this);
        } else {
          throw new Error("unexpected value type");
        }
      }
    };
  }

  function enumConstructor() {
    var type = function (value, name) {
      Enum.call(this, value, name);
    };
    inherits(type, Enum);
    return type;
  }

  function enumDesc(id, constructor) {
    var values = [];
    Object.keys(constructor).forEach(function (property) {
      var constant = constructor[property];
      if (constant instanceof Enum) {
        values[constant.value] = constant;
      }
    });
    constructor.TYPE_DESC = typeDesc(id, create(baseTypeHandler, {
      readBase: function (reader) {
        return values[reader.readVarInt()];
      },
      writeBase: function (value, writer) {
        writer.writeVarInt(value.value);
      }
    }));
  }

  function fieldHandler(field, typeHandler) {
    return {
      read: function (object, input) {
        object[field] = (typeHandler === null) ? input.read() : typeHandler.read(input);
      },
      write: function (id, object, output) {
        var value = object[field];
        if (value !== null) {
          output.writer.writeVarInt(id);
          if (typeHandler === null) {
            output.write(value);
          } else {
            typeHandler.write(value, output);
          }
        }
      }
    };
  }

  function classDesc(id, constructor) {
    var fieldId2handler = [];
    constructor.TYPE_DESC = typeDesc(id, create(typeHandler, {
      addField: function (id, handler) {
        fieldId2handler[id] = handler;
      },
      read: function (input) {
        var object = new constructor();
        while (true) {
          var id = input.reader.readVarInt();
          if (id === 0) {
            return object;
          }
          fieldId2handler[id].read(object, input);
        }
      },
      write: function (value, output) {
        fieldId2handler.forEach(function (handler, id) {
          handler.write(id, value, output);
        });
        output.writer.writeVarInt(0);
      }
    }));
  }

  function classField(constructor, id, name, typeDescOwner) {
    constructor.TYPE_DESC.handler.addField(
      id,
      fieldHandler(name, (typeDescOwner === null) ? null : typeDescOwner.TYPE_DESC.handler)
    );
  }

  function serializer(root) {
    var id2typeHandler = [];
    function addHandler(typeDescOwner) {
      var typeDesc = typeDescOwner.TYPE_DESC;
      id2typeHandler[typeDesc.id] = typeDesc.handler;
    }
    addHandler(NULL);
    addHandler(LIST);
    addHandler(BOOLEAN);
    addHandler(INTEGER);
    addHandler(STRING);
    function addPackage(root) {
      for (var name in root) {
        if (root.hasOwnProperty(name)) {
          var property = root[name];
          if (property.hasOwnProperty("TYPE_DESC")) {
            addHandler(property);
          } else {
            addPackage(property);
          }
        }
      }
    }
    addPackage(root);
    return {
      read: function (reader) {
        return new input(reader, id2typeHandler).read();
      },
      write: function (value, writer) {
        new output(writer).write(value);
      }
    };
  }

  return {
    inherits: inherits,
    writer: writer,
    reader: reader,
    Class: Class,
    Enum: Enum,
    LIST: LIST,
    BOOLEAN: BOOLEAN,
    INTEGER: INTEGER,
    STRING: STRING,
    enumConstructor: enumConstructor,
    enumDesc: enumDesc,
    classDesc: classDesc,
    classField: classField,
    serializer: serializer,
    service: function (id, implementation /* , interceptors... */) { // $todo
    },
    proxy: function (session, id /* , interceptors... */) { // $todo
    },
    rpc: function (result, callback) { // $todo
      callback(result);
    }
  };
}());
