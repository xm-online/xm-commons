import org.slf4j.LoggerFactory

def log = LoggerFactory.getLogger(getClass())
String prefix = "#### "

def data = lepContext.inArgs.event.data

log.info("${prefix} Commnas empty implementations, with params = {}", prefix, data)
