/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.mgrtomaszzurawski.ksef.sdk.api;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.lang.reflect.ParameterizedType;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Asserts that the public API surface of {@code ksef-client} does not
 * leak any of:
 *
 * <ul>
 *   <li>OpenAPI-generated {@code *Raw} types
 *       ({@code io.github.mgrtomaszzurawski.ksef.client.model.*Raw})</li>
 *   <li>Internal SDK types
 *       ({@code io.github.mgrtomaszzurawski.ksef.sdk.internal.*})</li>
 * </ul>
 *
 * <p>Public API surface is defined as: every {@code public}
 * constructor / method / field reachable from a class in any
 * non-{@code internal}, non-{@code client} package under
 * {@code io.github.mgrtomaszzurawski.ksef.sdk}.
 *
 * <p>The test scans classes by walking the classpath. Failure means a
 * regression that violates ADR-005 (no raw on public API) or ADR-021
 * (no Tier 3 escape hatch in 1.0).
 *
 * <p>Spec citation: Step 8 of
 * {@code context/IMPLEMENTATION-PLAN-1.0.0-2026-05-03-1712.md}.
 */
class PublicApiSurfaceTest {

    private static final String SDK_ROOT_PACKAGE = "io.github.mgrtomaszzurawski.ksef.sdk";
    private static final String INTERNAL_PACKAGE = "io.github.mgrtomaszzurawski.ksef.sdk.internal";
    private static final String GENERATED_PACKAGE = "io.github.mgrtomaszzurawski.ksef.client";
    private static final String RAW_SUFFIX = "Raw";
    /**
     * Allow-list — types intentionally documented as test seams in 1.0.0.
     * Tracked for removal in 1.1.0 per
     * {@code context/TESTKIT-MIGRATION-PLAYBOOK-2026-05-03-1850.md}
     * and ADR-020. Each entry must be {@code @Deprecated(forRemoval = true)}.
     */
    private static final Set<String> ALLOW_LIST_DEPRECATED_SEAMS = Set.of(
            "io.github.mgrtomaszzurawski.ksef.sdk.KsefClientInternals");

    @Test
    void publicSdkSurface_doesNotLeakInternalOrRawTypes() throws Exception {
        List<Class<?>> publicSdkClasses = scanPublicSdkClasses();
        List<String> violations = new ArrayList<>();

        for (Class<?> cls : publicSdkClasses) {
            if (ALLOW_LIST_DEPRECATED_SEAMS.contains(cls.getName())) {
                continue;
            }
            checkClass(cls, violations);
        }

        if (!violations.isEmpty()) {
            fail("Public API surface leaks internal/raw types:\n  - " + String.join("\n  - ", violations));
        }
    }

    private static void checkClass(Class<?> cls, List<String> violations) {
        for (Method m : cls.getDeclaredMethods()) {
            if (!isPublic(m.getModifiers())) {
                continue;
            }
            checkType("Method " + cls.getName() + "." + m.getName() + " return", m.getGenericReturnType(), violations);
            for (Parameter p : m.getParameters()) {
                checkType("Method " + cls.getName() + "." + m.getName() + " param " + p.getName(),
                        p.getParameterizedType(), violations);
            }
            for (AnnotatedType ex : m.getAnnotatedExceptionTypes()) {
                checkType("Method " + cls.getName() + "." + m.getName() + " throws", ex.getType(), violations);
            }
        }
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            if (!isPublic(c.getModifiers())) {
                continue;
            }
            for (Parameter p : c.getParameters()) {
                checkType("Constructor " + cls.getName() + " param " + p.getName(),
                        p.getParameterizedType(), violations);
            }
        }
        for (Field f : cls.getDeclaredFields()) {
            if (!isPublic(f.getModifiers())) {
                continue;
            }
            checkType("Field " + cls.getName() + "." + f.getName(), f.getGenericType(), violations);
        }
    }

    private static boolean isPublic(int modifiers) {
        return Modifier.isPublic(modifiers);
    }

    private static void checkType(String context, Type type, List<String> violations) {
        if (type instanceof Class<?> klass) {
            checkClassType(context, klass, violations);
        } else if (type instanceof ParameterizedType pt) {
            checkType(context, pt.getRawType(), violations);
            for (Type arg : pt.getActualTypeArguments()) {
                checkType(context, arg, violations);
            }
        } else if (type instanceof WildcardType wt) {
            for (Type bound : wt.getUpperBounds()) {
                checkType(context, bound, violations);
            }
            for (Type bound : wt.getLowerBounds()) {
                checkType(context, bound, violations);
            }
        }
        // GenericArrayType and TypeVariable handled implicitly by reflection
    }

    private static void checkClassType(String context, Class<?> klass, List<String> violations) {
        String name = klass.getName();
        if (name.startsWith(INTERNAL_PACKAGE)) {
            violations.add(context + " references internal type: " + name);
        } else if (name.startsWith(GENERATED_PACKAGE)) {
            violations.add(context + " references generated client type: " + name);
        } else if (name.endsWith(RAW_SUFFIX) && name.startsWith(SDK_ROOT_PACKAGE)) {
            violations.add(context + " references *Raw type: " + name);
        }
    }

    private static List<Class<?>> scanPublicSdkClasses() throws IOException, ClassNotFoundException {
        Set<String> classNames = new HashSet<>();
        ClassLoader cl = PublicApiSurfaceTest.class.getClassLoader();
        Enumeration<URL> roots = cl.getResources(SDK_ROOT_PACKAGE.replace('.', '/'));
        while (roots.hasMoreElements()) {
            URL root = roots.nextElement();
            switch (root.getProtocol()) {
                case "file" -> scanFileRoot(URI.create(root.toString()), classNames);
                case "jar" -> scanJarRoot(root, classNames);
                default -> { /* unknown protocol — skip */ }
            }
        }

        List<Class<?>> result = new ArrayList<>();
        for (String name : classNames) {
            if (name.startsWith(INTERNAL_PACKAGE)) {
                continue;
            }
            try {
                Class<?> cls = Class.forName(name, false, cl);
                if (Modifier.isPublic(cls.getModifiers())) {
                    result.add(cls);
                }
            } catch (NoClassDefFoundError | ClassNotFoundException ignored) {
                // class with missing transitive deps — out of scope
            }
        }
        return result;
    }

    private static void scanFileRoot(URI rootUri, Set<String> classNames) throws IOException {
        java.nio.file.Path root = java.nio.file.Paths.get(rootUri);
        if (!java.nio.file.Files.isDirectory(root)) {
            return;
        }
        try (var stream = java.nio.file.Files.walk(root)) {
            stream
                    .filter(p -> p.toString().endsWith(".class"))
                    .forEach(p -> {
                        java.nio.file.Path rel = root.getParent().getParent().getParent().getParent().getParent()
                                .getParent().relativize(p);
                        // simpler: derive package from absolute path
                        String absolute = p.toString();
                        int idx = absolute.indexOf("io/github/mgrtomaszzurawski/ksef/sdk");
                        if (idx < 0) {
                            return;
                        }
                        String relative = absolute.substring(idx);
                        String name = relative.replace('/', '.').replaceAll("\\.class$", "");
                        classNames.add(name);
                    });
        }
    }

    private static void scanJarRoot(URL jarUrl, Set<String> classNames) throws IOException {
        String jarPath = jarUrl.getPath();
        int bang = jarPath.indexOf('!');
        if (bang < 0) {
            return;
        }
        String filePath = jarPath.substring("file:".length(), bang);
        try (JarFile jar = new JarFile(filePath)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith("io/github/mgrtomaszzurawski/ksef/sdk/") || !name.endsWith(".class")) {
                    continue;
                }
                classNames.add(name.replace('/', '.').replaceAll("\\.class$", ""));
            }
        }
    }
}
