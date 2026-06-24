import org.springframework.http.HttpHeaders
import org.springframework.web.util.UriComponentsBuilder


log.info("Init lep engine for tenant {}", tenant)

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

