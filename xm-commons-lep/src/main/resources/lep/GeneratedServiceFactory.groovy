import __fullClassName__
import org.springframework.util.ClassUtils

Class type = __simpleClassName__.class
if (ClassUtils.hasConstructor(type)) {
    return new __simpleClassName__();
} else {
    return new __simpleClassName__(lepContext);
}
