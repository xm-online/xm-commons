import org.springframework.util.ClassUtils

Class type = lepContext.inArgs.type
if (ClassUtils.hasConstructor(type)) {
    def ctor = type.getConstructor();
    return ctor.newInstance();
} else {
    def ctor = type.getConstructors()[0];
    def parameter = ctor.getParameters()[0]
    def lepContextType = parameter.getType()
    return ctor.newInstance(lepContextType.cast(lepContext))
}
