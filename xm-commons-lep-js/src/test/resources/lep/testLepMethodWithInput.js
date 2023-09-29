function main(lepContext) {
    return "Hello " + lepContext.inArgs.get('inputMap').get('testLepService').testLepMethod();
}
main(lepContext)
