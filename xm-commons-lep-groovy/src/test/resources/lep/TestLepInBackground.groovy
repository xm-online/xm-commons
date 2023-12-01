import java.util.concurrent.CompletableFuture

threadUtils = lepContext.commons.threadUtils()
result = lepContext.inArgs.input.result

return CompletableFuture.supplyAsync({ ->
    return threadUtils.executeInNewContext({
        result.set(lepContext.commons.service())
        return "SUCCESS"
    })
}).get()
