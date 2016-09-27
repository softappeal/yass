from collections import OrderedDict
from enum import Enum
from io import StringIO
from struct import Struct
from typing import cast, Any, Dict, List, TypeVar, Generic, Optional, Callable, Set


def abstract(abstractClass):  # todo: implement for Python 2.7
    return abstractClass


DOUBLE_STRUCT = Struct('>d')
INT_STRUCT = Struct('>i')


class Writer:
    def __init__(self, writeBytes):  # type: (Callable[[bytes], None]) -> None
        self.writeBytes = writeBytes

    def writeByte(self, value):  # type: (int) -> None
        self.writeBytes(chr(value))

    def writeVarInt(self, value):  # type: (int) -> None
        while True:
            if (value & 0xffffff80) == 0:
                self.writeByte(value)
                return
            self.writeByte((value & 0x7f) | 0x80)
            value = (value if value >= 0 else (value + 0x100000000)) >> 7

    def writeZigZagInt(self, value):  # type: (int) -> None
        self.writeVarInt((value << 1) ^ (value >> 31))

    def writeInt(self, value):  # type: (int) -> None
        self.writeBytes(INT_STRUCT.pack(value))

    def writeDouble(self, value):  # type: (float) -> None
        self.writeBytes(DOUBLE_STRUCT.pack(value))


class Reader:
    def __init__(self, readBytes):  # type: (Callable[[int], bytes]) -> None
        self.readBytes = readBytes

    def readByte(self):  # type: () -> int
        return ord(self.readBytes(1))

    def readVarInt(self):  # type: () -> int
        shift = 0
        value = 0
        while shift < 32:
            b = self.readByte()
            value |= ((b & 0x7f) << shift)
            if (b & 0x80) == 0:
                return value if value <= 0x7fffffff else value - 0x100000000
            shift += 7
        raise RuntimeError("malformed VarInt input")

    def readZigZagInt(self):  # type: () -> int
        value = self.readVarInt()
        return ((value if value >= 0 else (value + 0x100000000)) >> 1) ^ -(value & 1)

    def readInt(self):  # type: () -> int
        return INT_STRUCT.unpack(self.readBytes(4))[0]

    def readDouble(self):  # type: () -> float
        return DOUBLE_STRUCT.unpack(self.readBytes(8))[0]


@abstract
class TypeHandler:
    def read(self, input):  # type: (Input) -> Optional[Any]
        raise NotImplementedError()

    def write(self, value, output):  # type: (Optional[Any], Output) -> None
        raise NotImplementedError()

    def writeWithId(self, id, value, output):  # type: (int, Optional[Any], Output) -> None
        output.writer.writeVarInt(id)
        self.write(value, output)


class TypeDesc:
    def __init__(self, id, handler):  # type: (int, TypeHandler) -> None
        self.id = id
        self.handler = handler

    def write(self, value, output):  # type: (Optional[Any], Output) -> None
        self.handler.writeWithId(self.id, value, output)


TYPE_DESC = "TYPE_DESC"


def typeDesc(value):  # type: (Any) -> TypeDesc
    return getattr(value, TYPE_DESC)


class Input:
    def __init__(self, reader, id2typeHandler):  # type: (Reader, Dict[int, TypeHandler]) -> None
        self.reader = reader
        self.id2typeHandler = id2typeHandler
        self.referenceableObjects = None  # type: Optional[List[Any]]

    def read(self):  # type: () -> Optional[Any]
        return self.id2typeHandler[self.reader.readVarInt()].read(self)


class Output:
    def __init__(self, writer):  # type: (Writer) -> None
        self.writer = writer
        self.object2reference = None  # type: Optional[Dict[Any, int]]

    def write(self, value):  # type: (Optional[Any]) -> None
        if value is None:
            NULL_DESC.write(None, self)
        elif isinstance(value, list):
            LIST_DESC.write(value, self)
        elif isinstance(value, bool):
            BOOLEAN_DESC.write(value, self)
        elif isinstance(value, float):
            DOUBLE_DESC.write(value, self)
        elif isinstance(value, unicode):
            STRING_DESC.write(value, self)
        elif isinstance(value, str):
            BYTES_DESC.write(value, self)
        else:
            try:
                td = typeDesc(value)
            except:
                raise RuntimeError("missing type %s in serializer" % value.__class__)
            td.write(value, self)


class NullTypeHandler(TypeHandler):
    def read(self, input):  # type: (Input) -> None
        return None

    def write(self, value, output):  # type: (None, Output) -> None
        pass


class ReferenceTypeHandler(TypeHandler):
    def read(self, input):  # type: (Input) -> Any
        return input.referenceableObjects[input.reader.readVarInt()]

    def write(self, value, output):  # type: (int, Output) -> None
        output.writer.writeVarInt(value)


class ListTypeHandler(TypeHandler):
    def read(self, input):  # type: (Input) -> List[Any]
        return [input.read() for dummy in range(input.reader.readVarInt())]

    def write(self, value, output):  # type: ( List[Any], Output) -> None
        output.writer.writeVarInt(len(value))
        for e in value:
            output.write(e)


@abstract
class BaseTypeHandler(TypeHandler):
    def readBase(self, reader):  # type: (Reader) -> Any
        raise NotImplementedError()

    def read(self, input):  # type: (Input) -> Any
        return self.readBase(input.reader)

    def writeBase(self, value, writer):  # type: (Any, Writer) -> None
        raise NotImplementedError()

    def write(self, value, output):  # type: (Any, Output) -> None
        self.writeBase(value, output.writer)


class BooleanTypeHandler(BaseTypeHandler):
    def readBase(self, reader):  # type: (Reader) -> bool
        return reader.readByte() != 0

    def writeBase(self, value, writer):  # type: (bool, Writer) -> None
        writer.writeByte(1 if value else 0)


class DoubleTypeHandler(BaseTypeHandler):
    def readBase(self, reader):  # type: (Reader) -> float
        return reader.readDouble()

    def writeBase(self, value, writer):  # type: (float, Writer) -> None
        writer.writeDouble(value)


class StringTypeHandler(BaseTypeHandler):
    def readBase(self, reader):  # type: (Reader) -> unicode
        return unicode(reader.readBytes(reader.readVarInt()), 'utf-8')

    def writeBase(self, value, writer):  # type: (unicode, Writer) -> None
        b = value.encode('utf-8')
        writer.writeVarInt(len(b))
        writer.writeBytes(b)


class BytesTypeHandler(BaseTypeHandler):
    def readBase(self, reader):  # type: (Reader) -> bytes
        return reader.readBytes(reader.readVarInt())

    def writeBase(self, value, writer):  # type: (bytes, Writer) -> None
        writer.writeVarInt(len(value))
        writer.writeBytes(value)


NULL_DESC = TypeDesc(0, NullTypeHandler())
REFERENCE_DESC = TypeDesc(1, ReferenceTypeHandler())
LIST_DESC = TypeDesc(2, ListTypeHandler())
BOOLEAN_DESC = TypeDesc(3, BooleanTypeHandler())
DOUBLE_DESC = TypeDesc(4, DoubleTypeHandler())
STRING_DESC = TypeDesc(5, StringTypeHandler())
BYTES_DESC = TypeDesc(6, BytesTypeHandler())

FIRST_DESC_ID = 7


class EnumTypeHandler(BaseTypeHandler):
    def __init__(self, type):  # type: (Any) -> None
        self.type = type

    def readBase(self, reader):  # type: (Reader) -> Any
        return self.type(reader.readVarInt())

    def writeBase(self, value, writer):  # type: (Any, Writer) -> None
        writer.writeVarInt(value.value)


def enumDesc(id, type):  # type: (int, Any) -> None
    setattr(type, TYPE_DESC, TypeDesc(id, EnumTypeHandler(type)))


class FieldHandler:
    def __init__(self, field, typeHandler):  # type: (str,  Optional[TypeHandler]) -> None
        self.field = field
        self.typeHandler = typeHandler

    def read(self, object, input):  # type: (Any, Input) -> None
        setattr(object, self.field, input.read() if self.typeHandler is None else self.typeHandler.read(input))

    def write(self, id, object, output):  # type: (int, Any, Output) -> None
        value = getattr(object, self.field)
        if value is not None:
            output.writer.writeVarInt(id)
            if self.typeHandler is None:
                output.write(value)
            else:
                self.typeHandler.write(value, output)


class FieldDesc:
    def __init__(self, id, field, typeInfo):  # type: (int, str, Optional[Any]) -> None
        self.id = id
        if typeInfo is None:
            self.handler = FieldHandler(field, None)
        elif isinstance(typeInfo, TypeDesc):
            self.handler = FieldHandler(field, typeInfo.handler)
        else:
            self.handler = FieldHandler(field, typeDesc(typeInfo).handler)


class ClassTypeHandler(TypeHandler):
    FIELD_END = 0

    def __init__(self, type, referenceable):  # type: (Any, bool) -> None
        self.type = type
        self.referenceable = referenceable
        self.id2fieldHandler = OrderedDict()  # type: Dict[int, FieldHandler]

    def addField(self, id, handler):  # type: (int, FieldHandler) -> None
        self.id2fieldHandler[id] = handler

    def read(self, input):  # type: (Input) -> None
        object = self.type()
        if self.referenceable:
            if input.referenceableObjects is None:
                input.referenceableObjects = []
            input.referenceableObjects.append(object)
        while True:
            id = input.reader.readVarInt()
            if id == ClassTypeHandler.FIELD_END:
                return object
            self.id2fieldHandler[id].read(object, input)

    def write(self, value, output):  # type: (Any, Output) -> None
        for id, handler in self.id2fieldHandler.items():
            handler.write(id, value, output)
        output.writer.writeVarInt(ClassTypeHandler.FIELD_END)

    def writeWithId(self, id, value, output):  # type: (int, Any, Output) -> None
        if self.referenceable:
            if output.object2reference is None:
                output.object2reference = {}
            object2reference = output.object2reference
            reference = object2reference.get(value)
            if reference is not None:
                REFERENCE_DESC.write(reference, output)
                return
            object2reference[value] = len(object2reference)
        TypeHandler.writeWithId(self, id, value, output)


def fieldDescs(type, fieldDescs):  # type: (Any, List[FieldDesc]) -> None
    handler = cast(ClassTypeHandler, typeDesc(type).handler)
    for fieldDesc in fieldDescs:
        handler.addField(fieldDesc.id, fieldDesc.handler)


def classDesc(id, type, referenceable):  # type: (int, Any, bool) -> None
    setattr(type, TYPE_DESC, TypeDesc(id, ClassTypeHandler(type, referenceable)))


@abstract
class Serializer:
    def read(self, reader):  # type: (Reader) -> Optional[Any]
        raise NotImplementedError()

    def write(self, value, writer):  # type: (Optional[Any], Writer) -> None
        raise NotImplementedError()


class FastSerializer(Serializer):
    def __init__(self, typeInfos):  # type: ( List[Any]) -> None
        self.id2typeHandler = {}  # type: Dict[int, TypeHandler]
        for typeInfo in [NULL_DESC, REFERENCE_DESC, LIST_DESC, BOOLEAN_DESC, DOUBLE_DESC, STRING_DESC, BYTES_DESC] + typeInfos:
            td = typeInfo if isinstance(typeInfo, TypeDesc) else typeDesc(typeInfo)
            self.id2typeHandler[td.id] = td.handler

    def read(self, reader):  # type: (Reader) -> Optional[Any]
        return Input(reader, self.id2typeHandler).read()

    def write(self, value, writer):  # type: (Optional[Any], Writer) -> None
        Output(writer).write(value)


@abstract
class Message:
    pass


class Request(Message):
    def __init__(self, serviceId, methodId, arguments):  # type: (int, int, List[Optional[Any]]) -> None
        self.serviceId = serviceId
        self.methodId = methodId
        self.arguments = arguments


@abstract
class Reply(Message):
    def process(self):  # type: () -> Optional[Any]
        raise NotImplementedError()


class ValueReply(Reply):
    def __init__(self, value):  # type: (Optional[Any]) -> None
        self.value = value

    def process(self):  # type: () -> Optional[Any]
        return self.value


class ExceptionReply(Reply):
    def __init__(self, exception):  # type: (Exception) -> None
        self.exception = exception

    def process(self):  # type: () -> None
        raise self.exception


class MethodMapping:
    def __init__(self, id, method, oneWay):  # type: (int, str, bool) -> None
        self.id = id
        self.method = method
        self.oneWay = oneWay


class MethodMapper:
    def __init__(self, mappings):  # type: (List[MethodMapping]) -> None
        self.id2mapping = {}  # type: Dict[int, MethodMapping]
        self.method2Mapping = {}  # type: Dict[str, MethodMapping]
        for mapping in mappings:
            self.id2mapping[mapping.id] = mapping
            self.method2Mapping[mapping.method] = mapping

    def mapId(self, id):  # type: (int) -> Optional[MethodMapping]
        return self.id2mapping.get(id)

    def mapMethod(self, method):  # type: (str) -> MethodMapping
        return self.method2Mapping.get(method)


MAPPER = "MAPPER"


def methodMapper(type, mappings):
    setattr(type, MAPPER, MethodMapper(mappings))


Invocation = Callable[[], Optional[Any]]
Interceptor = Callable[
    [MethodMapping, List[Optional[Any]], Invocation],  # mapping, arguments, invocation
    Optional[Any]
]


def directInterceptor(mapping, arguments, invocation):  # type: (MethodMapping, List[Optional[Any]], Invocation) -> Optional[Any]
    return invocation()


class Service:
    def __init__(self, contractId, implementation, interceptor):  # type: (ContractId[Any], Any, Interceptor) -> None
        self.id = contractId.id
        self.implementation = implementation
        self.interceptor = interceptor
        self.mapper = contractId.mapper

    def invoke(self, request):  # type: (Request) -> Reply
        mapping = self.mapper.mapId(request.methodId)
        if mapping is None:
            raise RuntimeError("no methodId %s found for serviceId %s" % (request.methodId, request.serviceId))
        try:
            return ValueReply(self.interceptor(
                mapping,
                request.arguments,
                lambda: getattr(self.implementation, mapping.method)(*request.arguments)
            ))
        except Exception as e:
            return ExceptionReply(e)


class Server:
    def __init__(self, services):  # type: (List[Service]) -> None
        self.id2service = {}  # type: Dict[int, Service]
        for service in services:
            id = service.id
            if id in self.id2service:
                raise RuntimeError("serviceId %s already added" % id)
            self.id2service[id] = service

    def invoke(self, request):  # type: (Request) -> Reply
        service = self.id2service.get(request.serviceId)
        if service is None:
            raise RuntimeError("no serviceId %s found" % request.serviceId)
        return service.invoke(request)


C = TypeVar('C')


class ContractId(Generic[C]):
    def __init__(self, contract, id):  # type: (Any, int) -> None
        self.mapper = getattr(contract, MAPPER)  # type: MethodMapper
        self.id = id

    def service(self, implementation, interceptor=directInterceptor):  # type: (C, Interceptor) -> Service
        return Service(self, implementation, interceptor)


@abstract
class Client:
    def proxy(self, contractId, interceptor=directInterceptor):  # type: (ContractId[C], Interceptor) -> C
        client = self

        class Proxy:
            def __getattr__(self, method):  # type: (str) -> Any
                mapping = contractId.mapper.mapMethod(method)
                if mapping is None:
                    raise RuntimeError("no method '%s' found for serviceId %s" % (method, contractId.id))

                class Method:
                    def __call__(self, *arguments):
                        args = list(arguments)
                        return interceptor(
                            mapping,
                            args,
                            lambda: client.invoke(Request(contractId.id, mapping.id, args)).process()
                        )

                return Method()

        return cast(C, Proxy())

    def invoke(self, request):  # type: (Request) -> Reply
        raise NotImplementedError()


class MessageSerializer(Serializer):
    REQUEST = 0
    VALUE_REPLY = 1
    EXCEPTION_REPLY = 2

    def __init__(self, contractSerializer):  # type: (Serializer) -> None
        self.contractSerializer = contractSerializer

    def read(self, reader):  # type: (Reader) -> Message
        type = reader.readByte()
        if type == MessageSerializer.REQUEST:
            return Request(reader.readZigZagInt(), reader.readZigZagInt(), self.contractSerializer.read(reader))
        elif type == MessageSerializer.VALUE_REPLY:
            return ValueReply(self.contractSerializer.read(reader))
        else:
            return ExceptionReply(self.contractSerializer.read(reader))

    def write(self, message, writer):  # type: (Message, Writer) -> None
        if isinstance(message, Request):
            writer.writeByte(MessageSerializer.REQUEST)
            writer.writeZigZagInt(message.serviceId)
            writer.writeZigZagInt(message.methodId)
            self.contractSerializer.write(message.arguments, writer)
        elif isinstance(message, ValueReply):
            writer.writeByte(MessageSerializer.VALUE_REPLY)
            self.contractSerializer.write(message.value, writer)
        else:
            writer.writeByte(MessageSerializer.EXCEPTION_REPLY)
            self.contractSerializer.write(cast(ExceptionReply, message).exception, writer)


class SimpleTransportSetup:
    def __init__(self, messageSerializer, server):  # type: (Serializer, Server) -> None
        self.messageSerializer = messageSerializer
        self.server = server


class SimplePathResolver:
    def __init__(self, pathMappings):  # type: (Dict[Any, SimpleTransportSetup]) -> None
        self.pathMappings = pathMappings

    def resolvePath(self, path):  # type: (Any) -> SimpleTransportSetup
        setup = self.pathMappings.get(path)
        if setup is None:
            raise RuntimeError("no mapping for path '%s'" % path)
        return setup


class PathSerializer(Serializer):
    DEFAULT = 0

    def read(self, reader):  # type: (Reader) -> int
        return reader.readInt()

    def write(self, value, writer):  # type: (int, Writer) -> None
        writer.writeInt(value)


@abstract
class Stream:
    def writeBytes(self, value):  # type: (bytes) -> None
        raise NotImplementedError()

    def writeDone(self):  # type: () -> None
        pass

    def readBytes(self, length):  # type: (int) -> bytes
        raise NotImplementedError()

    def readDone(self):  # type: () -> None
        pass


class ServerTransport:
    def __init__(self, pathSerializer, pathResolver):  # type: (Serializer, SimplePathResolver) -> None
        self.pathSerializer = pathSerializer
        self.pathResolver = pathResolver

    def invoke(self, stream):  # type: (Stream) -> None
        reader = Reader(stream.readBytes)
        setup = self.pathResolver.resolvePath(self.pathSerializer.read(reader))
        request = setup.messageSerializer.read(reader)
        stream.readDone()
        setup.messageSerializer.write(setup.server.invoke(request), Writer(stream.writeBytes))
        stream.writeDone()


def defaultServerTransport(contractSerializer, server):  # type: (Serializer, Server) -> ServerTransport
    return ServerTransport(
        PathSerializer(),
        SimplePathResolver({
            PathSerializer.DEFAULT: SimpleTransportSetup(MessageSerializer(contractSerializer), server)
        })
    )


class ClientTransport:
    def __init__(self, pathSerializer, path, messageSerializer):  # type: (Serializer, Any, Serializer) -> None
        self.pathSerializer = pathSerializer
        self.path = path
        self.messageSerializer = messageSerializer

    def invoke(self, request, stream):  # type: (Request, Stream) -> Reply
        writer = Writer(stream.writeBytes)
        self.pathSerializer.write(self.path, writer)
        self.messageSerializer.write(request, writer)
        stream.writeDone()
        reply = self.messageSerializer.read(Reader(stream.readBytes))
        stream.readDone()
        return reply


def defaultClientTransport(contractSerializer):  # type: (Serializer) -> ClientTransport
    return ClientTransport(PathSerializer(), PathSerializer.DEFAULT, MessageSerializer(contractSerializer))


class Dumper:
    def __init__(self, compact, referenceables, concreteValueClasses=set()):  # type: (bool, bool, Set[Any]) -> None
        """
        :param compact: one-liner or multiple lines
        :param referenceables: True: dumps graphs (objects are marked with #); False: dumps trees
        :param concreteValueClasses: only allowed if (referenceables); these objects should not reference others; do not print # for these classes
        """
        if (not referenceables) and (len(concreteValueClasses) != 0):
            raise RuntimeError("concreteValueClasses only allowed if (referenceables)")
        self.compact = compact
        self.referenceables = referenceables
        self.concreteValueClasses = concreteValueClasses

    def dumpValueClass(self, value, write):  # type: (Any, Callable[[unicode], None]) -> bool
        """
        Could dump a value class (these should not reference other objects). Should be an one-liner.
        This implementation does nothing and returns False.
        :return True: if we dumped value; False: use default implementation
        """
        return False

    def isConcreteValueClass(self, value):  # type: (Any) -> bool
        return value.__class__ in self.concreteValueClasses

    def dump(self, value, write):  # type: (Optional[Any], Callable[[unicode], None]) -> None
        alreadyDumped = {} if self.referenceables else None  # type: Optional[Dict[int, Any]]
        tabs = [0]

        def dumpValue(value):  # type: (Optional[Any]) -> None
            if value is None:
                write(u"null")
            elif isinstance(value, unicode):
                write(u'"' + value + u'"')
            elif isinstance(value, (bool, float)):
                write(unicode(value))
            elif isinstance(value, str):
                write(u"b" + unicode(repr(value)))
            elif isinstance(value, Enum):
                write(unicode(value.name))
            elif isinstance(value, list):
                if self.compact:
                    write(u"[ ")
                    for element in value:
                        dumpValue(element)
                        write(u" ")
                    write(u"]")
                else:
                    write(u"[\n")
                    tabs[0] += 1
                    for element in value:
                        write(unicode(tabs[0] * "    "))
                        dumpValue(element)
                        write(u"\n")
                    tabs[0] -= 1
                    write(unicode(tabs[0] * "    ") + u"]")
            else:
                referenceables = self.referenceables and (not self.isConcreteValueClass(value))
                index = 0
                if referenceables:
                    index = alreadyDumped.get(value)
                    if index is not None:
                        write(u"#" + unicode(index))
                        return
                    index = len(alreadyDumped)
                    alreadyDumped[value] = index
                if not self.dumpValueClass(value, write):
                    if self.compact:
                        write(value.__class__.__name__ + u"( ")
                        for name, value in sorted(value.__dict__.items(), key=lambda item: item[0]):
                            if value is not None:
                                write(name + u"=")
                                dumpValue(value)
                                write(u" ")
                        write(u")")
                    else:
                        write(value.__class__.__name__ + u"(\n")
                        tabs[0] += 1
                        for name, value in sorted(value.__dict__.items(), key=lambda item: item[0]):
                            if value is not None:
                                write(unicode(tabs[0] * "    ") + name + u" = ")
                                dumpValue(value)
                                write(u"\n")
                        tabs[0] -= 1
                        write(unicode(tabs[0] * "    ") + u")")
                if referenceables:
                    write(u"#" + unicode(index))

        dumpValue(value)

    def toString(self, value):  # type: (Optional[Any]) -> unicode
        io = StringIO()

        def write(s):  # type: (unicode) -> None
            io.write(s)

        self.dump(value, write)
        return io.getvalue()
