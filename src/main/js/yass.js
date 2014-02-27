// $todo: review

var yass = (function () {
  'use strict';

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

  function create(proto, props) { // $todo: is there a better way ?
    var o = Object.create(proto);
    for (var p in props) {
      if (props.hasOwnProperty(p)) {
        o[p] = props[p];
      }
    }
    return o;
  }

  var baseTypeHandler = {
    read: function (input) {
      return this.readBase(input.reader);
    },
    write: function (value, output) {
      this.writeBase(value, output.writer);
    }
  };

  function typeDescOwner(id, proto, props) {
    var o = proto ? create(proto, props) : props;
    o.TYPE_DESC = typeDesc(id, o);
    return o;
  }

  var NULL = typeDescOwner(0, null, {
    read: function (input) {
      return null;
    },
    write: function (value, output) {
      // empty
    }
  });

  var LIST = typeDescOwner(2, null, {
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
        } else if ((value instanceof Enum) || (value instanceof Class)) {
          value.constructor.TYPE_DESC.write(value, this);
        } else {
          throw new Error("unexpected value");
        }
      }
    };
  }

  function inherits(child, parent) { // $todo: is this ok ?
    child.prototype = Object.create(parent.prototype);
    child.prototype.constructor = child;
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
        object[field] = typeHandler ? typeHandler.read(input) : input.read();
      },
      write: function (id, object, output) {
        var value = object[field];
        if (value) {
          output.writer.writeVarInt(id);
          if (typeHandler) {
            typeHandler.write(value, output);
          } else {
            output.write(value);
          }
        }
      }
    };
  }

  function classDesc(id, constructor) {
    var fieldId2handler = [];
    constructor.TYPE_DESC = typeDesc(id, {
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
    });
  }

  function classField(constructor, id, name, typeDescOwner) {
    constructor.TYPE_DESC.handler.addField(id, fieldHandler(name, typeDescOwner && typeDescOwner.TYPE_DESC.handler));
  }

  function serializer(typeDescOwners) {
    var id2typeHandler = [];
    [NULL, LIST, BOOLEAN, INTEGER, STRING].concat(typeDescOwners).forEach(function (typeDescOwner) {
      var typeDesc = typeDescOwner.TYPE_DESC;
      id2typeHandler[typeDesc.id] = typeDesc.handler;
    });
    return {
      read: function (reader) {
        return new input(reader, id2typeHandler).read();
      },
      write: function (value, writer) {
        new output(writer).write(value);
      }
    };
  }

  function direct(method, parameters, proceed) {
    return proceed();
  }

  function composite(intercept1, intercept2) {
    if (intercept1 === direct) {
      return intercept2;
    }
    if (intercept2 === direct) {
      return intercept1;
    }
    return function (method, parameters, proceed) {
      return intercept1(method, parameters, function () {
        return intercept2(method, parameters, proceed);
      });
    };
  }

  function context() {
    return {
      value: null, // $todo: hide this somehow
      hasInvocation: function () {
        return this.value !== null;
      },
      get: function () {
        if (this.value === null) {
          throw new Error("no active invocation");
        }
        return this.value;
      }
    };
  }

  function contextIntercept(context, value) {
    return function (method, parameters, proceed) {
      var oldValue = context.value;
      context.value = value;
      try {
        return proceed();
      } finally {
        context.value = oldValue;
      }
    };
  }

  function request(serviceId, methodId, parameters) {
    return {
      serviceId: serviceId,
      methodId: methodId,
      parameters: parameters
    };
  }

  function valueReply(value) {
    return {
      process: function () {
        return value;
      }
    };
  }

  function exceptionReply(throwable) {
    return {
      process: function () {
        throw throwable;
      }
    };
  }

  function methodMapping(method, id, oneWay) {
    return {
      method: method,
      id: id,
      oneWay: oneWay
    };
  }

  function methodMapper(contract, methodMappings) {
    var id2mapping = [];
    var name2Mapping = {};
    methodMappings.forEach(function (mapping) {
      id2mapping[mapping.id] = mapping;
      name2Mapping[mapping.method] = mapping;
    });
    return {
      mapId: function (id) {
        return id2mapping[id];
      },
      mapMethod: function (method) {
        return name2Mapping[method];
      },
      proxy: function (intercept) {
        var stub = {};
        function delegate(method) {
          stub[method] = function () {
            return intercept(method, arguments);
          };
        }
        for (var method in name2Mapping) {
          if (name2Mapping.hasOwnProperty(method)) {
            delegate(method);
          }
        }
        return stub;
      }
    };
  }

  var contractIdContext = context();

  function contractId(id, methodMapper) {
    var ci = {
      methodMapper: methodMapper,
      id: id,
      service: function (implementation, intercept) {
        return service(this, implementation, intercept);
      },
      invoker: function (client) {
        return client.invoker(this);
      }
    };
    ci.intercept = contextIntercept(contractIdContext, ci); // $todo: hide somehow
    return ci;
  }

  function clientInvocation(invocationIntercept, serviceId, methodMapping, parameters) {
    return {
      oneWay: methodMapping.oneWay,
      invoke: function (intercept, tunnel) {
        var that = this;
        return composite(intercept, invocationIntercept)(methodMapping.method, parameters, function () {
          var reply = tunnel(request(serviceId, methodMapping.id, parameters));
          return that.oneWay ? null : reply.process();
        });
      }
    };
  }

  function client() {
    return {
      invoke: function (clientInvocation) {
        throw new Error("abstract method called");
      },
      invoker: function (contractId) {
        var that = this;
        return function (proxyIntercept) {
          var intercept = composite(contractId.intercept, proxyIntercept);
          return contractId.methodMapper.proxy(function (method, parameters) {
            return that.invoke(
              clientInvocation(intercept, contractId.id, contractId.methodMapper.mapMethod(method), parameters)
            );
          });
        };
      }
    };
  }

  function service(contractId, implementation, intercept) {
    return {
      contractId: contractId,
      implementation: implementation,
      intercept: intercept
    };
  }

  function serverInvocation(serverInvoker, request) {
    var methodMapping = serverInvoker.methodMapper.mapId(request.methodId);
    var method = methodMapping.method;
    return {
      oneWay: methodMapping.oneWay,
      invoke: function (intercept) {
        return serverInvoker.invoke(intercept, method, request.parameters);
      }
    };
  }

  function serverInvoker(service) {
    var invokerIntercept = composite(service.contractId.intercept, service.intercept);
    var implementation = service.implementation;
    return {
      methodMapper: service.contractId.methodMapper,
      invoke: function (invocationIntercept, method, parameters) {
        var proceed = function () {
          return implementation[method].apply(implementation, parameters);
        };
        var intercept = composite(invocationIntercept, invokerIntercept);
        var value;
        try {
          value = intercept(method, parameters, proceed);
        } catch (t) {
          return exceptionReply(t);
        }
        return valueReply(value);
      }
    };
  }

  function server(services) {
    var serviceId2invoker = [];
    services.forEach(function (service) {
      var id = service.contractId.id;
      if (serviceId2invoker[id]) {
        throw new Error("serviceId '" + id + "' already added");
      }
      serviceId2invoker[id] = serverInvoker(service);
    });
    return {
      invocation: function (request) {
        var invoker = serviceId2invoker[request.serviceId];
        if (!invoker) {
          throw new Error("no serviceId '" + request.serviceId + "' found (methodId '" + request.methodId + "')");
        }
        return serverInvocation(invoker, request);
      }
    };
  }

  return {
    writer: writer,
    reader: reader,
    Class: Class,
    Enum: Enum,
    create: create,
    LIST: LIST,
    BOOLEAN: BOOLEAN,
    INTEGER: INTEGER,
    STRING: STRING,
    inherits: inherits,
    enumConstructor: enumConstructor,
    enumDesc: enumDesc,
    classDesc: classDesc,
    classField: classField,
    serializer: serializer,
    direct: direct,
    composite: composite,
    context: context,
    contextIntercept: contextIntercept,
    contractId: contractId,
    methodMapping: methodMapping,
    methodMapper: methodMapper,
    contractIdContext: contractIdContext,
    server: server,
    service: service,
    client: client,
    rpc: function (result, callback) { // $todo
      callback(result);
    }
  };
}());
