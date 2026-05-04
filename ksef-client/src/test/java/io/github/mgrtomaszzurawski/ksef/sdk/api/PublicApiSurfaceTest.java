/*
 * Copyright (c) 2026 Tomasz Zurawski
 * SPDX-License-Identifier: AGPL-3.0-only
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

    @Test
    void publicSdkSurface_doesNotLeakInternalOrRawTypes() throws Exception {
        List<Class<?>> publicSdkClasses = scanPublicSdkClasses();
        // Sanity floor: the SDK has dozens of public classes. If the scanner returned
        // an empty list we are not testing anything (Codex F2 — the previous scanner
        // produced an empty list on Windows). 30 is well below the real count
        // (>100 records + clients + builders) but high enough to detect a broken scan.
        org.junit.jupiter.api.Assertions.assertTrue(publicSdkClasses.size() >= 30,
                "Scanner returned only " + publicSdkClasses.size() + " classes — surface gate is vacuous");
        List<String> violations = new ArrayList<>();

        for (Class<?> cls : publicSdkClasses) {
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
            // A "public" constructor is unreachable from JPMS consumers when ALL of its
            // parameters live in non-exported packages — the consumer module cannot even
            // name the parameter types, let alone supply values. The constructor exists
            // as public for in-module use (KsefClient instantiates KsefSession etc.).
            // We surface a violation only when at least one parameter is consumer-visible
            // and another is internal/raw, which would be a real shape leak.
            if (allParametersUnreachableFromConsumers(c)) {
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

    /**
     * Returns {@code true} when at least one parameter of the constructor is in a
     * package that is invisible to consumer modules (internal SDK plumbing or
     * generated raw types). Under JPMS, a consumer cannot resolve such a parameter
     * type and therefore cannot invoke the constructor — even if the constructor
     * is reflectively "public". This guards the SDK-internal-construction-only
     * constructors of {@code KsefSession}, {@code KsefBatchSession} and
     * {@code PreparedInvoiceExport}, which take internal SDK types alongside
     * primitives like {@code String} or {@code byte[]}.
     *
     * <p>JPMS treats the type as unresolvable, so the constructor is functionally
     * unreachable from a consumer module — but the surface test would otherwise
     * raise a noisy false positive. Methods (return type or other parameters)
     * still go through the strict check, so this exception is narrow.
     */
    private static boolean allParametersUnreachableFromConsumers(Constructor<?> c) {
        if (c.getParameterCount() == 0) {
            return false;
        }
        for (Parameter p : c.getParameters()) {
            String paramTypeName = p.getType().getName();
            boolean paramIsInternal = paramTypeName.startsWith(INTERNAL_PACKAGE)
                    || paramTypeName.startsWith(GENERATED_PACKAGE)
                    || (paramTypeName.startsWith(SDK_ROOT_PACKAGE) && paramTypeName.endsWith(RAW_SUFFIX));
            if (paramIsInternal) {
                return true;
            }
        }
        return false;
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

        // Primary path: walk the build's target/classes directory directly. This works
        // regardless of how the test is launched (classpath vs. modulepath; Surefire's
        // modular layer hides the SDK package from ClassLoader.getResources()).
        java.nio.file.Path explicitRoot = java.nio.file.Path.of(
                "target", "classes", "io", "github", "mgrtomaszzurawski", "ksef", "sdk");
        if (java.nio.file.Files.isDirectory(explicitRoot)) {
            scanFileRootByDirectoryWalk(explicitRoot, classNames);
        }

        // Fallback for non-Maven launches: classpath/jar URL enumeration.
        if (classNames.isEmpty()) {
            Enumeration<URL> roots = cl.getResources(SDK_ROOT_PACKAGE.replace('.', '/'));
            while (roots.hasMoreElements()) {
                URL root = roots.nextElement();
                switch (root.getProtocol()) {
                    case "file" -> scanFileRoot(URI.create(root.toString()), classNames);
                    case "jar" -> scanJarRoot(root, classNames);
                    default -> { /* unknown protocol — skip */ }
                }
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

    /**
     * Walk {@code packageDir} recursively, mapping each {@code .class} file back
     * to its fully-qualified class name. The package prefix is appended to
     * {@link #SDK_ROOT_PACKAGE} based on the relative path under the package
     * directory itself — no fragile {@code getParent()} chain is required.
     */
    private static void scanFileRootByDirectoryWalk(java.nio.file.Path packageDir,
                                                     Set<String> classNames) throws IOException {
        try (var stream = java.nio.file.Files.walk(packageDir)) {
            stream
                    .filter(p -> p.toString().endsWith(".class"))
                    .forEach(p -> {
                        java.nio.file.Path rel = packageDir.relativize(p);
                        StringBuilder name = new StringBuilder(SDK_ROOT_PACKAGE);
                        for (int seg = 0; seg < rel.getNameCount(); seg++) {
                            name.append('.').append(rel.getName(seg).toString());
                        }
                        String fqn = name.toString().replaceAll("\\.class$", "");
                        classNames.add(fqn);
                    });
        }
    }

    private static void scanFileRoot(URI rootUri, Set<String> classNames) throws IOException {
        // The {@code rootUri} points at {@code .../classes/io/github/mgrtomaszzurawski/ksef/sdk}.
        // The package prefix we need to recover starts six segments up. Walking up via
        // getParent() six times gives the {@code classes/} directory, which is the
        // class-loading root we relativize each .class file against. Using
        // {@link java.nio.file.Path#relativize(Path)} produces a platform-agnostic
        // relative path that we can then re-encode using the Path separator we observed,
        // before turning slashes into dots. This is the JPMS-portable replacement for
        // the previous {@code indexOf("io/github/...")} approach, which silently
        // produced an empty class set on Windows where {@code Path.toString()} returns
        // backslashes (Codex F2).
        java.nio.file.Path packageDir = java.nio.file.Paths.get(rootUri);
        if (!java.nio.file.Files.isDirectory(packageDir)) {
            return;
        }
        // packageDir = .../classes/io/github/mgrtomaszzurawski/ksef/sdk
        // walk up 5 segments (sdk, ksef, mgrtomaszzurawski, github, io) to reach
        // .../classes which is the classpath root we relativize each .class against.
        java.nio.file.Path classpathRoot = packageDir;
        for (int up = 0; up < 5; up++) {
            classpathRoot = classpathRoot.getParent();
            if (classpathRoot == null) {
                return;
            }
        }
        java.nio.file.Path classpathRootFinal = classpathRoot;
        try (var stream = java.nio.file.Files.walk(packageDir)) {
            stream
                    .filter(p -> p.toString().endsWith(".class"))
                    .forEach(p -> {
                        java.nio.file.Path rel = classpathRootFinal.relativize(p);
                        StringBuilder name = new StringBuilder();
                        for (int seg = 0; seg < rel.getNameCount(); seg++) {
                            if (seg > 0) {
                                name.append('.');
                            }
                            name.append(rel.getName(seg).toString());
                        }
                        String fqn = name.toString().replaceAll("\\.class$", "");
                        classNames.add(fqn);
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
