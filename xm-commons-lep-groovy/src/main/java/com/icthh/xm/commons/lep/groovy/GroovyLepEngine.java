package com.icthh.xm.commons.lep.groovy;

import com.icthh.xm.commons.lep.LepPathResolver;
import com.icthh.xm.commons.lep.ProceedingLep;
import com.icthh.xm.commons.lep.XmLepConstants;
import com.icthh.xm.commons.lep.api.BaseLepContext;
import com.icthh.xm.commons.lep.api.LepEngine;
import com.icthh.xm.commons.lep.api.LepKey;
import com.icthh.xm.commons.lep.api.XmLepConfigFile;
import com.icthh.xm.commons.lep.groovy.storage.LepStorage;
import com.icthh.xm.commons.lep.impl.LoggingWrapper;
import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyClassLoader;
import groovy.lang.MetaClass;
import groovy.lang.MetaClassImpl;
import groovy.lang.MetaMethod;
import groovy.util.GroovyScriptEngine;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.reflection.CachedMethod;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.callsite.CallSite;
import org.codehaus.groovy.runtime.callsite.CallSiteArray;
import org.springframework.util.function.SingletonSupplier;

import static com.icthh.xm.commons.lep.groovy.storage.LepStorage.FILE_EXTENSION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
public class GroovyLepEngine extends LepEngine {

    public static final Pattern PACKAGE_PATTERN = Pattern.compile("^package\\s+([\\w.]+);?", Pattern.MULTILINE);
    public static final String LEP_PREFIX = "lep://";
    public static final String COMMONS_SCRIPT = "/Commons$$";
    public static final String CLASSNAME_REGEX = "[$.\\-]";
    public static final String INIT_SCRIPT_NAME = "InitLepEngine.groovy";
    public static final String INIT_SCRIPT;

    /**
     * The init script is a fixed resource of this library that only patches a few meta classes - it references
     * nothing of a tenant, so its class does not have to belong to the classloader of an engine. Compiling it
     * once per JVM keeps every engine creation, and there is one per config refresh per tenant, from
     * recompiling the very same source over and over.
     */
    private static final GroovyClassLoader INIT_SCRIPT_CLASS_LOADER =
        new GroovyClassLoader(GroovyLepEngine.class.getClassLoader());
    private static final SingletonSupplier<Class<?>> INIT_SCRIPT_CLASS =
        SingletonSupplier.of(GroovyLepEngine::compileInitScript);

    private final String tenant;
    private final LepStorage leps;
    private final GroovyScriptEngine gse;
    private final LoggingWrapper loggingWrapper;
    private final LepPathResolver lepPathResolver;
    private final List<String> tenantCommonsFolders;
    private final Boolean useDirectoryCompiledSources;

    private final Map<String, GroovyFileParser.GroovyFileMetadata> lepMetadata = new ConcurrentHashMap<>();

    static {
        INIT_SCRIPT = loadFile(INIT_SCRIPT_NAME);
    }

    @SneakyThrows
    private static String loadFile(String name) {
        InputStream content = GroovyLepEngine.class.getClassLoader().getResourceAsStream(name);
        return IOUtils.toString(content, UTF_8);
    }

    public GroovyLepEngine(String engineId,
                           String tenant,
                           LepStorage leps,
                           LoggingWrapper loggingWrapper,
                           ClassLoader classLoader,
                           Map<String, GroovyFileParser.GroovyFileMetadata> lepMetadata,
                           LepResourceConnector lepResourceConnector,
                           LepPathResolver lepPathResolver,
                           boolean isWarmupEnabled,
                           boolean useDirectoryCompiledSources,
                           String targetDirectoryPath,
                           int minimumRecompilationInterval) {
        super(engineId);
        this.tenant = tenant;
        this.leps = leps;
        this.loggingWrapper = loggingWrapper;
        this.useDirectoryCompiledSources = useDirectoryCompiledSources;
        this.gse = buildGroovyEngine(classLoader, lepResourceConnector, targetDirectoryPath, minimumRecompilationInterval);
        this.lepMetadata.putAll(lepMetadata);
        this.lepPathResolver = lepPathResolver;
        this.tenantCommonsFolders = lepPathResolver.getLepCommonsPaths(tenant);
        runInitScript();
        if (isWarmupEnabled) {
            warmupScripts();
        } else {
            log.warn("Warmup lep script for tenant {} disabled", tenant);
        }
    }

    protected GroovyScriptEngine buildGroovyEngine(ClassLoader classLoader,
                                                   LepResourceConnector lepResourceConnector,
                                                   String targetDirectoryPath,
                                                   int minimumRecompilationInterval) {
        GroovyScriptEngine gse;
        CompilerConfiguration config;
        if (useDirectoryCompiledSources) {
            File targetDir = new File(targetDirectoryPath);
            gse = new GroovyScriptEngine(lepResourceConnector, buildCachingClassLoader(classLoader, targetDir));
            config = gse.getConfig();
            config.setTargetDirectory(targetDir);
        } else {
            gse = new GroovyScriptEngine(lepResourceConnector, classLoader);
            config = gse.getConfig();
        }

        config.setRecompileGroovySource(true);
        config.setMinimumRecompilationInterval(minimumRecompilationInterval);
        config.getOptimizationOptions().put(CompilerConfiguration.INVOKEDYNAMIC, false);
        gse.setConfig(config);
        GroovyClassLoader groovyClassLoader = gse.getGroovyClassLoader();
        groovyClassLoader.setShouldRecompile(true);
        // GroovyScriptEngine wraps the default resource loader with connector-first lookup but keeps a
        // fallback that scans the whole application classpath for a ".groovy" source of every class name
        // the compiler or the metaclass machinery fails to resolve (e.g. groovy.runtime.metaclass.*MetaClass,
        // *Customizer of java.beans). Every lep source is served by the connector, so the fallback can never
        // find anything - it only costs a full classpath scan per unresolved name on every engine build and
        // on the first call of every lep
        groovyClassLoader.setResourceLoader(className -> findLepSource(lepResourceConnector, className));
        return gse;
    }

    private static URL findLepSource(LepResourceConnector lepResourceConnector, String className) {
        try {
            return lepResourceConnector.getResourceConnection(className.replace('.', '/') + FILE_EXTENSION).getURL();
        } catch (Throwable notFound) {
            return null;
        }
    }

    private void runInitScript() {
        log.info("START run groovy lep engine init script for tenant {}", tenant);
        StopWatch stopWatch = StopWatch.createStarted();
        try {
            Binding binding = new Binding(new HashMap<>(Map.of(
                "log", log,
                "tenant", tenant
            )));
            InvokerHelper.createScript(initScriptClass(), binding).run();
            log.info("STOP groovy lep engine init script for tenant {}, time: {} ms",
                tenant, stopWatch.getTime(MILLISECONDS));
        } catch (Throwable e) {
            log.error("Error run groovy lep engine init script for tenant {}", tenant, e);
        }
    }

    /**
     * Compiles the init script on the first engine of the JVM and hands the very same class to every engine
     * after it. Each engine still runs it with its own binding, so the behaviour per tenant is unchanged.
     */
    static Class<?> initScriptClass() {
        return INIT_SCRIPT_CLASS.obtain();
    }

    private static Class<?> compileInitScript() {
        StopWatch stopWatch = StopWatch.createStarted();
        Class<?> scriptClass = INIT_SCRIPT_CLASS_LOADER.parseClass(INIT_SCRIPT, INIT_SCRIPT_NAME);
        log.info("Compile groovy lep engine init script {}, time: {} ms",
            INIT_SCRIPT_NAME, stopWatch.getTime(MILLISECONDS));
        return scriptClass;
    }

    private void warmupScripts() {
        StopWatch stopWatch = StopWatch.createStarted();
        log.info("Start warmup lep scripts");
        GroovyClassLoader groovyClassLoader = gse.getGroovyClassLoader();

        this.leps.forEach(lep -> {
            try {
                StopWatch warmUpTime = StopWatch.createStarted();
                log.info("START | Warmup lep {}", lep.getPath());

                if (useDirectoryCompiledSources && tryLoadCompiled(groovyClassLoader, lep, warmUpTime)) {
                    return;
                }

                Class<?> scriptClass = gse.loadScriptByName(LEP_PREFIX + lep.getPath());
                InvokerHelper.getMetaClass(scriptClass);
                log.info("STOP | Warmup lep {}, time: {} ms", lep.getPath(), warmUpTime.getTime(MILLISECONDS));
            } catch (Throwable e) {
                log.error("Error create script {}", lep.getPath(), e);
            }
        });

        warmupGroovyRuntimeState(groovyClassLoader);

        log.info("Stop warm-up LEP scripts, time = {} ms", stopWatch.getTime(MILLISECONDS));
    }

    private void warmupGroovyRuntimeState(GroovyClassLoader groovyClassLoader) {
        StopWatch stopWatch = StopWatch.createStarted();
        Class<?>[] classes = groovyClassLoader.getLoadedClasses();
        for (Class<?> clazz : classes) {
            try {
                MetaClass metaClass = InvokerHelper.getMetaClass(clazz);
                // closures are dispatched through their own path and dominate the class count of a big tenant
                if (metaClass instanceof MetaClassImpl metaClassImpl && !Closure.class.isAssignableFrom(clazz)) {
                    warmupCallSites(clazz, metaClassImpl);
                }
            } catch (Throwable e) {
                log.warn("Error warmup groovy runtime state of {}: {}", clazz.getName(), e.toString());
            }
        }
        log.info("Warmup groovy runtime state of {} classes, time = {} ms",
            classes.length, stopWatch.getTime(MILLISECONDS));
    }

    private void warmupCallSites(Class<?> clazz, MetaClassImpl metaClass) {
        for (MetaMethod method : metaClass.getMethods()) {
            if (method instanceof CachedMethod cachedMethod && isDeclaredBy(cachedMethod, clazz)) {
                warmupCallSite(clazz, metaClass, cachedMethod);
            }
        }
    }

    private void warmupCallSite(Class<?> clazz, MetaClassImpl metaClass, CachedMethod method) {
        try {
            CallSite callSite = new CallSiteArray(clazz, new String[]{method.getName()}).array[0];
            Class<?>[] parameterTypes = method.getNativeParameterTypes();
            if (method.isStatic()) {
                method.createStaticMetaMethodSite(callSite, metaClass, parameterTypes);
            } else {
                method.createPogoMetaMethodSite(callSite, metaClass, parameterTypes);
            }
        } catch (Throwable e) {
            log.debug("Error warmup callsite of {}.{}: {}", clazz.getName(), method.getName(), e.toString());
        }
    }

    private static boolean isDeclaredBy(CachedMethod method, Class<?> clazz) {
        return method.getDeclaringClass().getTheClass() == clazz;
    }

    @Override
    public boolean isExists(LepKey lepKey) {
        List<String> before = getBeforeKeys(lepKey);
        List<String> main = getMainKeys(lepKey);
        return getExistingKey(before).isPresent() || getExistingKey(main).isPresent();
    }

    @Override
    @SneakyThrows
    public Object invoke(LepKey lepKey, ProceedingLep lepMethod, BaseLepContext lepContext) {
        List<String> beforeKeys = getBeforeKeys(lepKey);
        getExistingKey(beforeKeys).ifPresent(key -> executeLep(key, lepKey, lepMethod, lepContext));

        List<String> mainKeys = getMainKeys(lepKey);
        Optional<String> mainKey = getExistingKey(mainKeys);
        if (mainKey.isPresent()) {
            return executeLep(mainKey.get(), lepKey, lepMethod, lepContext);
        } else {
            return lepContext.lep.proceed();
        }
    }

    @SneakyThrows
    private Object executeLep(String key, LepKey lepKey, ProceedingLep lepMethod, BaseLepContext lepContext) {
        String scriptName = LEP_PREFIX + key + FILE_EXTENSION;
        if (useDirectoryCompiledSources) {
            return loggingWrapper.doWithLogs(lepMethod, scriptName, lepKey, () -> {
                String className = toGroovyClassName(leps.getByPath(key));
                return InvokerHelper.createScript(
                    gse.getGroovyClassLoader().loadClass(className, true, false),
                    new Binding(new HashMap<>(Map.of("lepContext", lepContext)))
                ).run();
            });
        }
        return loggingWrapper.doWithLogs(lepMethod, scriptName, lepKey, () ->
            // map HAVE TO be mutable!
            gse.run(LEP_PREFIX + key, new Binding(new HashMap<>(Map.of("lepContext", lepContext))))
        );
    }

    private List<String> getMainKeys(LepKey lepKey) {
        String lepPath = lepPathResolver.getLepPath(lepKey, tenant);
        String legacyLepPath = lepPathResolver.getLegacyLepPath(lepKey, tenant);
        return List.of(
            legacyLepPath + "$$tenant",
            legacyLepPath + "$$around",
            lepPath + "$$tenant",
            lepPath + "$$around",
            legacyLepPath,
            lepPath
        );
    }

    private List<String> getBeforeKeys(LepKey lepKey) {
        String lepPath = lepPathResolver.getLepPath(lepKey, tenant);
        String legacyLepPath = lepPathResolver.getLegacyLepPath(lepKey, tenant);
        return List.of(
            legacyLepPath + "$$before",
            lepPath + "$$before"
        );
    }

    private Optional<String> getExistingKey(List<String> keys) {
        return keys.stream().filter(leps::isExists).findFirst();
    }

    private boolean isCommonsClass(String path) {
        return !path.contains(COMMONS_SCRIPT) && tenantCommonsFolders.stream().anyMatch(path::startsWith);
    }

    private boolean isScript(XmLepConfigFile lep) {
        return lepMetadata.containsKey(lep.metadataKey()) && lepMetadata.get(lep.metadataKey()).isScript();
    }

    private boolean tryLoadCompiled(GroovyClassLoader classLoader, XmLepConfigFile lep, StopWatch warmUpTime) {
        try {
            String className = toGroovyClassName(lep);
            Class<?> loadClass = classLoader.loadClass(className, true, false);
            InvokerHelper.getMetaClass(loadClass);
            log.info("PRECOMPILED | Lep compiled sources {}, time {} ms", lep.getPath(), warmUpTime.getTime(MILLISECONDS));
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private String toGroovyClassName(XmLepConfigFile lep) {
        String fileName = extractFileName(lep.getPath());
        String className = fileName.replaceAll(CLASSNAME_REGEX, "_");
        String packageName = extractPackageName(readString(lep));

        return packageName != null
            ? packageName + "." + className
            : className;
    }

    private String extractFileName(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    @SneakyThrows
    private URLClassLoader buildCachingClassLoader(ClassLoader parent, File targetDir) {
        List<URL> urls = new ArrayList<>();
        // new distribution layout: classes are packed into a jar next to the compiled directory,
        // URLClassLoader reads them directly from the jar without unpacking the class tree
        File compiledJar = new File(targetDir.getParentFile(), XmLepConstants.SCRIPT_COMPILED_JAR);
        if (compiledJar.isFile()) {
            urls.add(compiledJar.toURI().toURL());
        }
        // the directory stays on the classpath: old layout distributions keep classes there,
        // and it is the target directory for classes compiled at runtime
        urls.add(targetDir.toURI().toURL());
        return new URLClassLoader(urls.toArray(new URL[0]), parent);
    }

    @SneakyThrows
    private String readString(XmLepConfigFile value) {
        return IOUtils.toString(value.getContentStream().getInputStream(), UTF_8);
    }

    private String extractPackageName(String text) {
        var matcher = PACKAGE_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

}
