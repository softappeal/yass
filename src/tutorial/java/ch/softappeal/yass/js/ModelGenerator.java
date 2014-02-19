package ch.softappeal.yass.js;

import ch.softappeal.yass.Version;
import ch.softappeal.yass.core.remote.ContractId;
import ch.softappeal.yass.serialize.fast.AbstractFastSerializer;
import ch.softappeal.yass.serialize.fast.ClassTypeHandler;
import ch.softappeal.yass.serialize.fast.TypeHandler;
import ch.softappeal.yass.util.Check;

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

public final class ModelGenerator extends Generator {

  private final String rootPackage;
  private final String yassModule;
  private final String modelModule;
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
    tabs("%s = {", jsType(type));
    inc();
    boolean first = true;
    for (final Enum<?> e : type.getEnumConstants()) {
      if (!first) {
        print(",");
      }
      first = false;
      println();
      tabs("%s: %s", e.name(), e.ordinal());
    }
    dec();
    println();
    tabsln("};");
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
    tabsln("%s = function () {", jsType(type));
    inc();
    for (final Field field : fields) {
      if (s != null) {
        tabsln("%s.call(this);", jsType(s));
      }
      tabsln("this.%s = null;", field.getName());
    }
    dec();
    tabsln("};");
    if (s != null) {
      tabsln("%s.inherits(%s, %s);", yassModule, jsType(type), jsType(s));
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

  @SuppressWarnings("unchecked")
  public ModelGenerator(
    final Package rootPackage, final JsFastSerializer serializer, final String yassModule, final String modelModule, final String modelFile
  ) throws Exception {
    super(modelFile);
    visitedPackages.add(rootPackage.getName());
    this.rootPackage = rootPackage.getName() + '.';
    this.yassModule = Check.notNull(yassModule);
    this.modelModule = Check.notNull(modelModule);
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
    for (final Map.Entry<Integer, TypeHandler> entry : serializer.id2typeHandler().entrySet()) {
      final TypeHandler typeHandler = entry.getValue();
      final Class<?> type = typeHandler.type;
      if (type.isEnum()) {
        generateEnum((Class<Enum<?>>)type);
      } else if (typeHandler instanceof ClassTypeHandler) {
        generateClass(type);
      }
    }
    close();
  }

}
