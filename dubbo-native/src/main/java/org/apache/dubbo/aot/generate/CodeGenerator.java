/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.aot.generate;

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.AdaptiveClassCodeGenerator;
import org.apache.dubbo.common.extension.SPI;
import org.apache.dubbo.common.utils.StringUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * generate related self-adaptive code (native image does not support dynamic code generation. Therefore, code needs to be generated before compilation)
 */
public class CodeGenerator {

    private static final String PACKAGE_NAME_PREFIX = "org.apache.dubbo";

    public static void main(String[] args) {
        String generatedSources = args[1];

        List<Class<?>> classes = new ClassSourceFinder().findClassSet(PACKAGE_NAME_PREFIX).stream().map(it -> {
            try {
                return Class.forName(it);
            } catch (Throwable e) {
            }
            return null;
        }).collect(Collectors.toList());
        new ArrayList<>(classes).stream().filter(it -> {
            if (null == it) {
                return false;
            }
            Annotation anno = it.getAnnotation(SPI.class);
            if (null == anno) {
                return false;
            }
            Optional<Method> optional = Arrays.stream(it.getMethods()).filter(it2 -> it2.getAnnotation(Adaptive.class) != null).findAny();
            return optional.isPresent();
        }).forEach(it -> {
            SPI spi = it.getAnnotation(SPI.class);
            String value = spi.value();
            if (StringUtils.isEmpty(value)) {
                value = "adaptive";
            }
            AdaptiveClassCodeGenerator codeGenerator = new AdaptiveClassCodeGenerator(it, value);
            String code = codeGenerator.generate(true);
            try {
                String file = generatedSources + File.separator + it.getName().replaceAll("\\.", Matcher.quoteReplacement(File.separator));
                String dir = Paths.get(file).getParent().toString();
                FileUtils.forceMkdir(new File(dir));
                code = LICENSED_STR + code + "\n";
                FileUtils.write(new File(file + "$Adaptive.java"), code, Charset.defaultCharset());
            } catch (IOException e) {
                throw new IllegalStateException("Failed to generated adaptive class sources", e);
            }
        });
    }


    private static final String LICENSED_STR = "/*\n" +
        " * Licensed to the Apache Software Foundation (ASF) under one or more\n" +
        " * contributor license agreements.  See the NOTICE file distributed with\n" +
        " * this work for additional information regarding copyright ownership.\n" +
        " * The ASF licenses this file to You under the Apache License, Version 2.0\n" +
        " * (the \"License\"); you may not use this file except in compliance with\n" +
        " * the License.  You may obtain a copy of the License at\n" +
        " *\n" +
        " *     http://www.apache.org/licenses/LICENSE-2.0\n" +
        " *\n" +
        " * Unless required by applicable law or agreed to in writing, software\n" +
        " * distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
        " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
        " * See the License for the specific language governing permissions and\n" +
        " * limitations under the License.\n" +
        " */\n";

}
