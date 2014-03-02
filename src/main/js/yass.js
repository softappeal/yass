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

  function classDesc(id, Constructor) {
    var fieldId2handler = [];
    Constructor.TYPE_DESC = typeDesc(id, {
      addField: function (id, handler) {
        fieldId2handler[id] = handler;
      },
      read: function (input) {
        var object = new Constructor();
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

  function argsToArr(parameters) {
    var arrayParameters = [];
    for (var p = parameters.length - 1; p >= 0; p--) { // $todo: is there a better way to copy the arguments ?
      arrayParameters[p] = parameters[p];
    }
    return arrayParameters;
  }

  function serializer(typeDescOwners) {
    var id2typeHandler = [];
    [NULL, LIST, BOOLEAN, INTEGER, STRING].concat(argsToArr(arguments)).forEach(function (typeDescOwner) {
      var typeDesc = typeDescOwner.TYPE_DESC;
      id2typeHandler[typeDesc.id] = typeDesc.handler;
    });
    return {
      read: function (reader) {
        return input(reader, id2typeHandler).read();
      },
      write: function (value, writer) {
        output(writer).write(value);
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
      value: value,
      process: function () {
        return value;
      }
    };
  }

  function exceptionReply(exception) {
    return {
      exception: exception,
      process: function () {
        throw exception;
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

  function composite2(interceptor1, interceptor2) {
    return function (method, parameters, proceed) {
      return interceptor1(method, parameters, function () {
        return interceptor2(method, parameters, proceed);
      });
    };
  }

  var DIRECT = function (method, parameters, proceed) {
    return proceed();
  };

  function compositeSkip(start, interceptors) {
    var i1 = DIRECT;
    for (var i = start; i < interceptors.length; i++) {
      var i2 = interceptors[i];
      i1 = (i1 === DIRECT) ? i2 : ((i2 === DIRECT) ? i1 : composite2(i1, i2));
    }
    return i1;
  }

  function composite() {
    return compositeSkip(0, arguments);
  }

  function methodMapper() {
    var id2mapping = [];
    var name2Mapping = {};
    argsToArr(arguments).forEach(function (mapping) {
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
      proxy: function (interceptor) {
        var stub = {};
        function delegate(method) {
          stub[method] = function () {
            return interceptor(method, arguments);
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

  function contractId(id, methodMapper) {
    return {
      methodMapper: methodMapper,
      id: id,
      service: function () {
        return {
          contractId: this,
          implementation: arguments[0],
          interceptor: compositeSkip(1, arguments)
        };
      },
      invoker: function (client) {
        return client.invoker(this);
      }
    };
  }

  function processReply(reply, replyCallback) {
    if (replyCallback) {
      replyCallback(function () {
        return reply.process();
      });
    }
  }

  function clientInvocation(interceptor, serviceId, methodMapping, parameters) {
    return function (tunnel) {
      var replyCallback = null;
      if (!methodMapping.oneWay) {
        replyCallback = parameters[parameters.length - 1];
        if (typeof replyCallback !== "function") {
          throw new Error("calling method '" + methodMapping.method + "' without callback");
        }
      }
      var arrayParameters = []; // note: copy needed because parameters is instanceof Arguments and not a real array
      for (var p = parameters.length - (replyCallback ? 2 : 1); p >= 0; p--) { // $todo: is there a better way to copy the arguments ?
        arrayParameters[p] = parameters[p];
      }
      return interceptor(methodMapping.method, arrayParameters, function () {
        return tunnel(request(serviceId, methodMapping.id, arrayParameters), replyCallback);
      });
    };
  }

  function client() {
    return {
      invoker: function (contractId) {
        var that = this;
        return function () {
          var interceptor = compositeSkip(0, arguments);
          return contractId.methodMapper.proxy(function (method, parameters) {
            return that.invoke(
              clientInvocation(interceptor, contractId.id, contractId.methodMapper.mapMethod(method), parameters)
            );
          });
        };
      }
    };
  }

  function serverInvocation(serverInvoker, request) {
    var methodMapping = serverInvoker.methodMapper.mapId(request.methodId);
    var method = methodMapping.method;
    return {
      oneWay: methodMapping.oneWay,
      invoke: function () {
        return serverInvoker.invoke(method, request.parameters);
      }
    };
  }

  function serverInvoker(service) {
    var interceptor = service.interceptor;
    var implementation = service.implementation;
    return {
      methodMapper: service.contractId.methodMapper,
      invoke: function (method, parameters) {
        var proceed = function () {
          var result = implementation[method].apply(implementation, parameters);
          return (typeof result === "undefined") ? null : result;
        };
        var value;
        try {
          value = interceptor(method, parameters, proceed);
        } catch (exception) {
          return exceptionReply(exception);
        }
        return valueReply(value);
      }
    };
  }

  function server() {
    var serviceId2invoker = [];
    argsToArr(arguments).forEach(function (service) {
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

  var END_REQUESTNUMBER = 0;

  function packet(requestNumber, message) {
    return {
      requestNumber: requestNumber,
      message: message,
      isEnd: function () {
        return this.requestNumber === END_REQUESTNUMBER;
      }
    };
  }

  var END_PACKET = packet(END_REQUESTNUMBER, null);

  function messageSerializer(serializer) {
    var REQUEST = 0;
    var VALUE_REPLY = 1;
    var EXCEPTION_REPLY = 2;
    return {
      read: function (reader) {
        var type = reader.readByte();
        if (type === REQUEST) {
          return request(serializer.read(reader), serializer.read(reader), serializer.read(reader));
        }
        if (type === VALUE_REPLY) {
          return valueReply(serializer.read(reader));
        }
        return exceptionReply(serializer.read(reader));
      },
      write: function (message, writer) {
        if (message.hasOwnProperty("serviceId")) {// $todo is there a better solution ?
          writer.writeByte(REQUEST);
          serializer.write(message.serviceId, writer);
          serializer.write(message.methodId, writer);
          serializer.write(message.parameters, writer);
        } else if (message.hasOwnProperty("value")) {// $todo is there a better solution ?
          writer.writeByte(VALUE_REPLY);
          serializer.write(message.value, writer);
        } else {
          writer.writeByte(EXCEPTION_REPLY);
          serializer.write(message.exception, writer);
        }
      }
    };
  }

  function packetSerializer(messageSerializer) {
    return {
      read: function (reader) {
        var requestNumber = reader.readInt();
        return (requestNumber === END_REQUESTNUMBER) ? END_PACKET : packet(requestNumber, messageSerializer.read(reader));
      },
      write: function (packet, writer) {
        if (packet.isEnd()) {
          writer.writeInt(END_REQUESTNUMBER);
        } else {
          writer.writeInt(packet.requestNumber);
          messageSerializer.write(packet.message, writer);
        }
      }
    };
  }

  function transportSerializer(serializer) {
    return packetSerializer(messageSerializer(serializer));
  }

  function mockClient(server, serializer) {
    serializer = messageSerializer(serializer);
    function copy(value) {
      var w = writer(1024);
      serializer.write(value, w);
      var r = reader(w.getUint8Array());
      value = serializer.read(r);
      if (!r.isEmpty()) {
        throw new Error("reader is not empty");
      }
      return value;
    }
    return create(client(), {
      invoke: function (invocation) {
        return invocation(function (request, replyCallback) {
          var reply = server.invocation(copy(request)).invoke();
          if (replyCallback) {
            processReply(copy(reply), replyCallback);
          }
          return null;
        });
      }
    });
  }

  function createSession(server, session, connection) {
    var isClosed = false;
    var isOpened = false;
    var nextRequestNumber = END_REQUESTNUMBER;
    var requestNumber2replyCallback = [];
    return create(create(client(), {
      open: function () {
        isOpened = true;
        try {
          this.opened();
        } catch (e) {
          this.doClose(e);
        }
      },
      doClose: function (exception) {
        this.doCloseSend(false, exception);
      },
      doCloseSend: function (sendEnd, exception) {
        if (isClosed) {
          return;
        }
        isClosed = true;
        try {
          this.closed(exception);
          if (sendEnd) {
            connection.write(END_PACKET);
          }
        } finally {
          connection.closed();
        }
      },
      write: function (requestNumber, message) {
        if (isClosed) {
          throw new Error("session is already closed");
        }
        try {
          connection.write(packet(requestNumber, message));
        } catch (exception) {
          this.doClose(exception);
        }
      },
      close: function () {
        this.doCloseSend(true, null);
      },
      invoke: function (invocation) {
        if (!isOpened) {
          throw new Error("session is not yet opened");
        }
        var that = this;
        return invocation(function (request, replyCallback) {
          var requestNumber = ++nextRequestNumber; // $todo: implement 32bit signed int behaviour and skip END_REQUESTNUMBER
          that.write(requestNumber, request);
          if (replyCallback) {
            if (requestNumber2replyCallback[requestNumber]) {
              throw new Error("already waiting for requestNumber " + requestNumber);
            }
            requestNumber2replyCallback[requestNumber] = replyCallback;
          }
          return null;
        });
      },
      received: function (packet) {
        try {
          if (packet.isEnd()) {
            this.doClose(null);
            return;
          }
          if (packet.message.hasOwnProperty("serviceId")) { // request // $todo is there a better solution ?
            var invocation = server.invocation(packet.message);
            var reply = invocation.invoke();
            if (!invocation.oneWay) {
              this.write(packet.requestNumber, reply);
            }
          } else { // reply
            var replyCallback = requestNumber2replyCallback[packet.requestNumber];
            delete requestNumber2replyCallback[packet.requestNumber];
            processReply(packet.message, replyCallback);
          }
        } catch (exception) {
          this.doClose(exception);
        }
      }
    }), session);
  }

  function connect(url, serializer, server, sessionFactory) {
    serializer = transportSerializer(serializer);
    var ws = new WebSocket(url);
    ws.binaryType = "arraybuffer";
    ws.onopen = function () {
      var session = createSession(server, sessionFactory(), {
        write: function (packet) {
          var w = writer(1024);
          serializer.write(packet, w);
          ws.send(w.getUint8Array());
        },
        closed: function () {
          ws.close();
        }
      });
      ws.onmessage = function (evt) {
        var r = reader(evt.data);
        session.received(serializer.read(r));
        if (!r.isEmpty()) {
          throw new Error("reader is not empty");
        }
      };
      ws.onerror = function (evt) {
        session.doClose(new Error("onerror"));
      };
      ws.onclose = function () {
        session.doClose(new Error("onclose"));
      };
      session.open();
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
    composite: composite,
    contractId: contractId,
    methodMapping: methodMapping,
    methodMapper: methodMapper,
    server: server,
    mockClient: mockClient,
    connect: connect
  };
}());
