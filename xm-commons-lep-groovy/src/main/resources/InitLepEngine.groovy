import org.springframework.http.HttpHeaders
import org.springframework.web.util.UriComponentsBuilder

// Runs ONCE PER JVM (see GroovyLepEngine.applyInitScriptOnce) and must stay tenant agnostic:
// the "tenant" binding only names the trigger for logging. Everything below is JVM-global state.
log.info("Init lep engine, triggered by {}", tenant)

// ------------------------------------------------------------------------------------------------
// Global metaclass patches. Applied before any metaclass of the patched hierarchies initializes:
// groovy propagates an expando patch to a subclass only when the subclass metaclass initializes
// AFTER the patch, so this script must run before any warmup or lep touches these types.
// ------------------------------------------------------------------------------------------------

HttpHeaders.metaClass.collect = { Closure c ->
    log.warn("Method org.springframework.http.HttpHeaders.collect behavior changed, pls migrate to method forEach")
    delegate.headers.collect(c)
}

java.util.AbstractMap.metaClass.getProperties = { ->
    delegate.containsKey('properties')
            ? delegate.get('properties')
            : org.codehaus.groovy.runtime.DefaultGroovyMethods.getProperties(delegate)
}

UriComponentsBuilder.metaClass.static.fromHttpUrl = { String url ->
    return UriComponentsBuilder.fromUriString(url)
}

// ------------------------------------------------------------------------------------------------
// JVM-wide groovy runtime warmup. Executing representative idioms links the callsite and metaclass
// machinery that is created only by actually running code (string interpolation, collection and map
// dispatch, closures, property access) - otherwise the first lep execution of the JVM pays for it.
// ------------------------------------------------------------------------------------------------

int warmSum = 0
for (int i = 0; i < 3; i++) {
    warmSum += i
}
def warmList = [1, 2, 3].collect { it * 2 }.findAll { it > 2 }
def warmMap = [alpha: 1, beta: 2]
warmMap.gamma = warmMap.alpha + warmList.size()
def warmProperties = warmMap.properties // exercises the AbstractMap patch above
def warmText = "warmup-${warmSum}-${warmMap.gamma}".toString()
def warmClosure = { String value -> value.toUpperCase() }
warmClosure(warmText)
warmText.bytes.length
new BigDecimal(warmSum).add(BigDecimal.ONE)
try {
    throw new IllegalStateException(warmText)
} catch (IllegalStateException expected) {
    log.trace("Groovy exception path warmed: {}", expected.getMessage())
}
log.debug("Groovy runtime warmup executed: {} / {}", warmText, warmProperties == null ? 0 : 1)
