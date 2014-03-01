package ch.softappeal.yass.js;

import ch.softappeal.yass.Version;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.core.remote.MethodMapper;
import ch.softappeal.yass.serialize.fast.AbstractFastSerializer;
import ch.softappeal.yass.serialize.fast.ClassTypeHandler;
import ch.softappeal.yass.serialize.fast.JsFastSerializer;
import ch.softappeal.yass.serialize.fast.TypeDesc;
import ch.softappeal.yass.serialize.fast.TypeHandler;
import ch.softappeal.yass.util.Check;
import ch.softappeal.yass.util.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModelGenerator extends Generator { // $todo: review

  private final String rootPackage;
  private final String yassModule;
  private final String modelModule;
  private final Map<Integer, TypeHandler> id2typeHandler;
  private final Set<Class<?>> visitedClasses = new HashSet<>();
  private final Set<String> visitedPackages = new HashSet<>();
  private final List<String> types = new ArrayList<>();
  private final MethodMapper.Factory methodMapperFactory;

  private void generatePackage(final String aPackage) {
    if (visitedPackages.add(aPackage)) {
      generatePackage(aPackage.substring(0, aPackage.lastIndexOf('.')));
      tabsln("%s.%s = {};", modelModule, aPackage.substring(rootPackage.length()));
      println();
    }
  }

  private void checkType(final Class<?> type) {
    if (!type.getCanonicalName().startsWith(rootPackage)) {
      throw new RuntimeException('\'' + type.getCanonicalName() + "' has wrong root package");
    }
    generatePackage(type.getPackage().getName());
  }

  private String jsType(final Class<?> type) {
    return modelModule + '.' + type.getCanonicalName().substring(rootPackage.length());
  }

  private void generateEnum(final Class<? extends Enum<?>> type) {
    final String jsType = jsType(type);
    tabsln("%s = %s.enumConstructor();", jsType, yassModule);
    for (final Enum<?> e : type.getEnumConstants()) {
      tabsln("%s.%s = new %s(%s, \"%s\");", jsType, e.name(), jsType, e.ordinal(), e.name());
    }
    tabsln("%s.enumDesc(%s, %s);", yassModule, getId(type), jsType);
    types.add(jsType);
    println();
  }

  private void generateClass(final Class<?> type) {
    if (!visitedClasses.add(type)) {
      return;
    }
    checkType(type);
    Class<?> s = type.getSuperclass();
    if ((s == Object.class) || (s == Exception.class) || (s == RuntimeException.class) || (s == Error.class) || (s == Throwable.class)) {
      s = null;
    } else {
      generateClass(s);
    }
    final List<Field> fields = AbstractFastSerializer.ownFields(type);
    Collections.sort(fields, new Comparator<Field>() {
      @Override public int compare(final Field f1, final Field f2) {
        return f1.getName().compareTo(f2.getName());
      }
    });
    final String jsType = jsType(type);
    tabsln("%s = function () {", jsType);
    inc();
    for (final Field field : fields) {
      if (s != null) {
        tabsln("%s.call(this);", jsType(s));
      }
      tabsln("this.%s = null;", field.getName());
    }
    dec();
    tabsln("};");
    tabsln("%s.inherits(%s, %s);", yassModule, jsType, (s == null) ? (yassModule + ".Class") : jsType(s));
    final Integer id = getId(type);
    if (id != null) {
      tabsln("%s.classDesc(%s, %s);", yassModule, id, jsType);
      types.add(jsType);
    }
    println();
  }

  private void generateFields(final ClassTypeHandler typeHandler) {
    for (final ClassTypeHandler.FieldDesc fieldDesc : typeHandler.fieldDescs()) {
      final TypeHandler fieldHandler = fieldDesc.handler.typeHandler();
      final String typeDescOwner;
      if (TypeDesc.LIST.handler == fieldHandler) {
        typeDescOwner = yassModule + ".LIST";
      } else if (JsFastSerializer.BOOLEAN_TYPEDESC.handler == fieldHandler) {
        typeDescOwner = yassModule + ".BOOLEAN";
      } else if (JsFastSerializer.INTEGER_TYPEDESC.handler == fieldHandler) {
        typeDescOwner = yassModule + ".INTEGER";
      } else if (JsFastSerializer.STRING_TYPEDESC.handler == fieldHandler) {
        typeDescOwner = yassModule + ".STRING";
      } else if (fieldHandler == null) {
        typeDescOwner = "null";
      } else {
        typeDescOwner = jsType(fieldHandler.type);
      }
      tabsln(
        "%s.classField(%s, %s, \"%s\", %s);",
        yassModule, jsType(typeHandler.type), fieldDesc.id, fieldDesc.handler.field.getName(), typeDescOwner
      );
    }
    println();
  }

  private static final class ServiceDesc {
    final String name;
    final ContractId<?> contractId;
    ServiceDesc(final String name, final ContractId<?> contractId) {
      this.name = name;
      this.contractId = contractId;
    }
  }

  private static List<ServiceDesc> getServiceDescs(final Class<?> services) throws Exception {
    final List<ServiceDesc> serviceDescs = new ArrayList<>();
    for (final Field field : services.getFields()) {
      if (Modifier.isStatic(field.getModifiers()) && (field.getType() == ContractId.class)) {
        serviceDescs.add(new ServiceDesc(field.getName(), (ContractId<?>)field.get(null)));
      }
    }
    Collections.sort(serviceDescs, new Comparator<ServiceDesc>() {
      @Override public int compare(final ServiceDesc serviceDesc1, final ServiceDesc serviceDesc2) {
        return ((Integer)serviceDesc1.contractId.id).compareTo((Integer)serviceDesc2.contractId.id);
      }
    });
    return serviceDescs;
  }

  private static Set<Class<?>> getInterfaces(final Class<?> services) throws Exception {
    final List<ServiceDesc> serviceDescs = getServiceDescs(services);
    final Set<Class<?>> interfaces = new HashSet<>();
    for (final ServiceDesc serviceDesc : serviceDescs) {
      interfaces.add(serviceDesc.contractId.contract);
    }
    return interfaces;
  }

  private static Method[] getMethods(final Class<?> type) {
    final Method[] methods = type.getMethods();
    Arrays.sort(methods, new Comparator<Method>() {
      @Override public int compare(final Method method1, final Method method2) {
        return method1.getName().compareTo(method2.getName());
      }
    });
    return methods;
  }

  private void generateServices(final Class<?> services) throws Exception {
    tabs("%s = {", jsType(services));
    inc();
    boolean first = true;
    for (final ServiceDesc serviceDesc : getServiceDescs(services)) {
      final Class<?> type = serviceDesc.contractId.contract;
      if (!first) {
        print(",");
      }
      first = false;
      println();
      tabs(
        "%s: %s.contractId(%s, %s_MAPPER)",
        serviceDesc.name, yassModule, serviceDesc.contractId.id, jsType(type)
      );
    }
    dec();
    println();
    tabsln("};");
    println();
  }

  private void generateInterface(final Class<?> type) {
    checkType(type);
    final Method[] methods = getMethods(type);
    tabs("%s = {", jsType(type));
    inc();
    final MethodMapper methodMapper = methodMapperFactory.create(type);
    boolean firstMethod = true;
    for (final Method method : methods) {
      if (firstMethod) {
        firstMethod = false;
      } else {
        print(",");
      }
      println();
      tabs("%s: function (", method.getName());
      int param = 0; // $todo: use Parameter.getName() in Java 8
      for (final Class<?> paramType : method.getParameterTypes()) {
        if (param != 0) {
          print(", ");
        }
        print("param%s", param++);
      }
      print(") {}");
      if (methodMapper.mapMethod(method).oneWay) {
        print(" // OneWay");
      }
    }
    println();
    dec();
    tabsln("};");
    println();
    final String typeName = jsType(type);
    tabs("%s_MAPPER = %s.methodMapper(%s, [", typeName, yassModule, typeName);
    inc();
    boolean first = true;
    for (final Method method : getMethods(type)) {
      if (!first) {
        print(",");
      }
      first = false;
      println();
      final MethodMapper.Mapping mapping = methodMapper.mapMethod(method);
      tabs("%s.methodMapping(\"%s\", %s, %s)", yassModule, mapping.method.getName(), mapping.id, mapping.oneWay);
    }
    println();
    dec();
    println("]);");
    println();
  }

  @Nullable private Integer getId(final Class<?> type) {
    for (final Map.Entry<Integer, TypeHandler> entry : id2typeHandler.entrySet()) {
      if (entry.getValue().type == type) {
        return entry.getKey();
      }
    }
    return null;
  }

  /**
   * @param methodMapperFactory Note: You must provide a factory that doesn't allow overloading due to JavaScript restrictions!
   */
  @SuppressWarnings("unchecked")
  public ModelGenerator(
    final Package rootPackage, final JsFastSerializer serializer, final MethodMapper.Factory methodMapperFactory,
    final String yassModule, final String modelModule, final String modelFile
  ) throws Exception {
    super(modelFile);
    visitedPackages.add(rootPackage.getName());
    this.rootPackage = rootPackage.getName() + '.';
    this.yassModule = Check.notNull(yassModule);
    this.methodMapperFactory = Check.notNull(methodMapperFactory);
    this.modelModule = Check.notNull(modelModule);
    id2typeHandler = serializer.id2typeHandler();
    tabsln("// generated with %s %s", yassModule, Version.VALUE);
    println();
    tabsln("'use strict';");
    println();
    tabsln("var %s = {};", modelModule);
    println();
    for (final Map.Entry<Integer, TypeHandler> entry : id2typeHandler.entrySet()) {
      final TypeHandler typeHandler = entry.getValue();
      final Class<?> type = typeHandler.type;
      if (type.isEnum()) {
        generateEnum((Class<Enum<?>>)type);
      } else if (typeHandler instanceof ClassTypeHandler) {
        generateClass(type);
      }
    }
    final Class<?> clientServices = Class.forName(this.rootPackage + "ClientServices");
    final Class<?> serverServices = Class.forName(this.rootPackage + "ServerServices");
    final Set<Class<?>> interfacesSet = getInterfaces(clientServices);
    interfacesSet.addAll(getInterfaces(serverServices));
    final List<Class<?>> interfacesList = new ArrayList<>(interfacesSet);
    Collections.sort(interfacesList, new Comparator<Class<?>>() {
      @Override public int compare(final Class<?> type1, final Class<?> type2) {
        return type1.getCanonicalName().compareTo(type2.getCanonicalName());
      }
    });
    for (final Class<?> type : interfacesList) {
      generateInterface(type);
    }
    generateServices(clientServices);
    generateServices(serverServices);
    for (final Map.Entry<Integer, TypeHandler> entry : id2typeHandler.entrySet()) {
      final TypeHandler typeHandler = entry.getValue();
      if (typeHandler instanceof ClassTypeHandler) {
        generateFields((ClassTypeHandler)typeHandler);
      }
    }
    tabs("%s.SERIALIZER = %s.serializer([", modelModule, yassModule);
    inc();
    boolean first = true;
    for (final String type : types) {
      if (!first) {
        print(",");
      }
      first = false;
      println();
      tabs(type);
    }
    println();
    dec();
    tabsln("]);");
    close();
  }

}
