import groovy.util.logging.Slf4j

@Slf4j
class Service {
    Map<String, Object> lepContext

    public Service(Map<String, Object> lepContext) {
        log.info("lepContext.inArgs.input.parameter {}", lepContext.inArgs.input.parameter)
        this.lepContext = lepContext
    }
}
return new Service(lepContext).lepContext.inArgs.input.parameter
