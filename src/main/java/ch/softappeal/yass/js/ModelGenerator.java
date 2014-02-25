package ch.softappeal.yass.js;

import ch.softappeal.yass.Version;
import ch.softappeal.yass.core.remote.ContractId;
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

  private Set<Class<?>> generateServices(final String servicesName) throws Exception {
    class ServiceDesc {
      final String name;
      final ContractId<?> contractId;
      ServiceDesc(final String name, final ContractId<?> contractId) {
        this.name = name;
        this.contractId = contractId;
      }
    }
    final Class<?> services = Class.forName(rootPackage + servicesName);
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
    final Set<Class<?>> interfaces = new HashSet<>();
    tabs("%s = {", jsType(services));
    inc();
    boolean first = true;
    for (final ServiceDesc serviceDesc : serviceDescs) {
      if (!first) {
        print(",");
      }
      first = false;
      println();
      tabs("%s: %s /* %s */", serviceDesc.name, serviceDesc.contractId.id, jsType(serviceDesc.contractId.contract));
      interfaces.add(serviceDesc.contractId.contract);
    }
    dec();
    println();
    tabsln("};");
    println();
    return interfaces;
  }

  private void generateInterface(final Class<?> type) {
    checkType(type);
    final Method[] methods = type.getMethods();
    Arrays.sort(methods, new Comparator<Method>() {
      @Override public int compare(final Method method1, final Method method2) {
        return method1.getName().compareTo(method2.getName());
      }
    });
    tabsln("%s = function () {};", jsType(type));
    int param = 0;
    boolean first = true;
    for (final Method method : methods) {
      tabs("%s.prototype.%s = function (", jsType(type), method.getName());
      for (final Class<?> paramType : method.getParameterTypes()) {
        if (!first) {
          print(", ");
        }
        first = false;
        print("param%s", param++);
      }
      println(") {};");
    }
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

  @SuppressWarnings("unchecked")
  public ModelGenerator(
    final Package rootPackage, final JsFastSerializer serializer, final String yassModule, final String modelModule, final String modelFile
  ) throws Exception {
    super(modelFile);
    visitedPackages.add(rootPackage.getName());
    this.rootPackage = rootPackage.getName() + '.';
    this.yassModule = Check.notNull(yassModule);
    this.modelModule = Check.notNull(modelModule);
    id2typeHandler = serializer.id2typeHandler();
    tabsln("// generated with yass %s", Version.VALUE);
    println();
    tabsln("'use strict';");
    println();
    tabsln("var %s = {};", modelModule);
    println();
    final Set<Class<?>> interfacesSet = generateServices("ClientServices");
    interfacesSet.addAll(generateServices("ServerServices"));
    final List<Class<?>> interfacesList = new ArrayList<>(interfacesSet);
    Collections.sort(interfacesList, new Comparator<Class<?>>() {
      @Override public int compare(final Class<?> type1, final Class<?> type2) {
        return type1.getCanonicalName().compareTo(type2.getCanonicalName());
      }
    });
    for (final Class<?> type : interfacesList) {
      generateInterface(type);
    }
    for (final Map.Entry<Integer, TypeHandler> entry : id2typeHandler.entrySet()) {
      final TypeHandler typeHandler = entry.getValue();
      final Class<?> type = typeHandler.type;
      if (type.isEnum()) {
        generateEnum((Class<Enum<?>>)type);
      } else if (typeHandler instanceof ClassTypeHandler) {
        generateClass(type);
      }
    }
    for (final Map.Entry<Integer, TypeHandler> entry : id2typeHandler.entrySet()) {
      final TypeHandler typeHandler = entry.getValue();
      if (typeHandler instanceof ClassTypeHandler) {
        generateFields((ClassTypeHandler)typeHandler);
      }
    }
    tabsln("%s.SERIALIZER = new %s.Serializer(%s);", modelModule, yassModule, modelModule);
    close();
  }

}
