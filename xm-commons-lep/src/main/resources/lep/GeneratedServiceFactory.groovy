import org.springframework.util.ClassUtils

if (ClassUtils.hasConstructor(lepContext.inArgs.type)) {
    def ctor = lepContext.inArgs.type.getConstructor();
    return ctor.newInstance();
} else {
    def ctor = lepContext.inArgs.type.getConstructors()[0];
    return ctor.newInstance(lepContext)
}
