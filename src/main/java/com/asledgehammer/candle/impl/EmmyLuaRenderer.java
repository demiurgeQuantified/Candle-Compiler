package com.asledgehammer.candle.impl;

import com.asledgehammer.candle.*;
import com.asledgehammer.candle.yamldoc.YamlFile;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class EmmyLuaRenderer implements CandleRenderAdapter {

  String classNameLegalCurrent = "";

  CandleRenderer<CandleField> fieldRenderer =
      field ->
          "--- @field "
              + (field.isPublic() ? "public " : "")
              + field.getLuaName()
              + " "
              + field.getClazz().getSimpleName();

  CandleRenderer<CandleExecutableCluster<CandleConstructor>> constructorRenderer =
      cluster -> {
        List<CandleConstructor> constructors = cluster.getExecutables();
        CandleConstructor first = constructors.get(0);

        byte argOffset = 1;

        StringBuilder builder = new StringBuilder();
        builder.append("--- @public\n");
        if (first.isStatic()) builder.append("--- @static\n");

        StringBuilder paramBuilder = new StringBuilder();
        if (first.hasParameters()) {
          List<CandleParameter> parameters = first.getParameters();
          for (CandleParameter parameter : parameters) {
            String pName = parameter.getLuaName();
            if (pName.equals("true")) {
              pName = "arg" + argOffset++;
            }
            String pType = parameter.getJavaParameter().getType().getSimpleName();
            builder.append("--- @param ").append(pName).append(' ').append(pType).append('\n');
            paramBuilder.append(pName).append(", ");
          }
          paramBuilder.setLength(paramBuilder.length() - 2);
        }

        builder
            .append("--- @return ")
            .append(first.getExecutable().getDeclaringClass().getSimpleName())
            .append('\n');

        if (cluster.hasOverloads()) {
          for (int index = 1; index < constructors.size(); index++) {
            CandleConstructor overload = constructors.get(index);
            builder.append("--- @overload fun(");
            if (overload.hasParameters()) {
              List<CandleParameter> parameters = overload.getParameters();
              for (CandleParameter parameter : parameters) {
                builder
                    .append(parameter.getLuaName())
                    .append(": ")
                    .append(parameter.getJavaParameter().getType().getSimpleName())
                    .append(", ");
              }
              builder.setLength(builder.length() - 2);
            }
            builder.append("): ");
            builder.append(classNameLegalCurrent).append('\n');
          }
        }

        builder
            .append("function ")
            .append(classNameLegalCurrent)
            .append(".new(")
            .append(paramBuilder)
            .append(") end");

        return builder.toString();
      };

  CandleRenderer<CandleExecutableCluster<CandleMethod>> methodRenderer =
      cluster -> {
        List<CandleMethod> methods = cluster.getExecutables();
        CandleMethod first = methods.get(0);

        byte argOffset = 1;

        StringBuilder builder = new StringBuilder();
        builder.append("--- @public\n");
        if (first.isStatic()) builder.append("--- @static\n");

        StringBuilder paramBuilder = new StringBuilder();
        if (first.hasParameters()) {
          List<CandleParameter> parameters = first.getParameters();
          for (CandleParameter parameter : parameters) {
            String pName = parameter.getLuaName();
            if (pName.equals("true")) {
              pName = "arg" + argOffset++;
            }
            String pType = parameter.getJavaParameter().getType().getSimpleName();
            builder.append("--- @param ").append(pName).append(' ').append(pType).append('\n');
            paramBuilder.append(pName).append(", ");
          }
          paramBuilder.setLength(paramBuilder.length() - 2);
        }

        builder.append("--- @return ").append(first.getReturnType().getSimpleName()).append('\n');

        if (cluster.hasOverloads()) {
          for (int index = 1; index < methods.size(); index++) {
            CandleMethod overload = methods.get(index);
            builder.append("--- @overload fun(");
            if (overload.hasParameters()) {
              List<CandleParameter> parameters = overload.getParameters();
              for (CandleParameter parameter : parameters) {
                builder
                    .append(parameter.getLuaName())
                    .append(": ")
                    .append(parameter.getJavaParameter().getType().getSimpleName())
                    .append(", ");
              }
              builder.setLength(builder.length() - 2);
            }
            builder.append("): ");
            builder.append(overload.getReturnType().getSimpleName()).append('\n');
          }
        }

        builder
            .append("function ")
            .append(classNameLegalCurrent)
            .append(first.isStatic() ? '.' : ':')
            .append(cluster.getLuaName())
            .append("(")
            .append(paramBuilder)
            .append(") end");

        String resultCode = builder.toString();
        cluster.setRenderedCode(resultCode);
        return resultCode;
      };

  @Override
  public CandleRenderer<CandleClass> getClassRenderer() {
    return candleClass -> {
      Map<String, CandleField> fields = candleClass.getFields();
      Map<String, CandleExecutableCluster<CandleMethod>> methodsStatic =
          candleClass.getStaticMethods();
      Map<String, CandleExecutableCluster<CandleMethod>> methods = candleClass.getMethods();

//      for (String key : methodsStatic.keySet()) {
//        methodsStatic.get(key).sort();
//      }

//      for (String key : methods.keySet()) {
//        methods.get(key).sort();
//      }

      boolean alt = false;
      String className = candleClass.getLuaName();
      String classNameLegal = className;
      if (className.contains("$")) {
        classNameLegal = "_G['" + className + "']";
        alt = true;
      }

      classNameLegalCurrent = classNameLegal;

      Class<?> parentClass = candleClass.getClazz().getSuperclass();
      String parentName = parentClass != null ? parentClass.getSimpleName() : "";
      String superClazzName =
          parentClass != null && !parentName.equals("Object") ? ": " + parentName : "";

      StringBuilder builder = new StringBuilder("--- @meta\n\n");
      builder.append("--- @class ").append(className).append(superClazzName).append('\n');

      YamlFile yaml = candleClass.getYaml();

      if (yaml != null) {
        String notes = yaml.getNotes();
        if (notes != null) {
          builder.append("--- ").append(notes).append('\n');
        }
      }

      Class<?> clazz = candleClass.getClazz();
      Class<?>[] interfazes = clazz.getInterfaces();
      for (Class<?> interfaze : interfazes) {
        builder.append("--- @implement ").append(interfaze.getSimpleName()).append('\n');
      }

      if (!fields.isEmpty()) {
        List<String> keysSorted = new ArrayList<>(fields.keySet());
        keysSorted.sort(Comparator.naturalOrder());
        for (String fieldName : keysSorted) {
          builder.append(fieldRenderer.onRender(fields.get(fieldName))).append('\n');
        }
      }

      builder.append(classNameLegal).append(" = {};").append('\n');
      builder.append('\n');

      if (alt) {
        builder.append("local temp = ").append(classNameLegal).append(";\n");
      }

      if (!methodsStatic.isEmpty()) {
        builder.append("------------------------------------\n");
        builder.append("---------- STATIC METHODS ----------\n");
        builder.append("------------------------------------\n\n");
        List<String> keysSorted = new ArrayList<>(methodsStatic.keySet());
        keysSorted.sort(Comparator.naturalOrder());
        for (String fieldName : keysSorted) {
          builder
              .append(methodRenderer.onRender(methodsStatic.get(fieldName)))
              .append('\n')
              .append('\n');
        }
        builder.append('\n');
      }

      if (!methods.isEmpty()) {
        builder.append("------------------------------------\n");
        builder.append("------------- METHODS --------------\n");
        builder.append("------------------------------------\n\n");
        List<String> keysSorted = new ArrayList<>(methods.keySet());
        keysSorted.sort(Comparator.naturalOrder());
        for (String fieldName : keysSorted) {
          builder.append(methodRenderer.onRender(methods.get(fieldName))).append('\n').append('\n');
        }
        builder.append('\n');
      }

      if (candleClass.hasConstructors()) {
        builder.append("------------------------------------\n");
        builder.append("----------- CONSTRUCTOR ------------\n");
        builder.append("------------------------------------\n\n");

        CandleExecutableCluster<CandleConstructor> cluster = candleClass.getConstructors();
        builder.append(constructorRenderer.onRender(cluster));
        builder.append('\n');
      }

      return builder.toString();
    };
  }

  @Override
  public CandleRenderer<CandleAlias> getAliasRenderer() {
    return candleAlias -> "--- @class " + candleAlias.getLuaName();
  }
}
