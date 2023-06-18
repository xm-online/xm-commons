import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

ExecutorService executor = Executors.newSingleThreadExecutor();
Callable<String> callable = (Callable<String>) {
    lepContext.inArgs.input.testLepService.testLepMethod()
}
Future<String> future = lepContext.thread.runInThread(executor, callable);
return future.get();
