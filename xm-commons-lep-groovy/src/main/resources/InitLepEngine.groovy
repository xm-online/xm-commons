import org.springframework.http.HttpHeaders

log.info("Init lep engine for tenant {}", tenant)

HttpHeaders.metaClass.collect = { Closure c ->
    delegate.headers.collect(c)
}
