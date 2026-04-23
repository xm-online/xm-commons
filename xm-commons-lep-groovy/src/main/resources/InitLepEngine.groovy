import org.springframework.http.HttpHeaders

log.info("Init lep engine for tenant {}", tenant)

HttpHeaders.metaClass.collect = { Closure c ->
    log.warn("Method org.springframework.http.HttpHeaders.collect behavior changed, pls migrate to method forEach")
    delegate.headers.collect(c)
}
