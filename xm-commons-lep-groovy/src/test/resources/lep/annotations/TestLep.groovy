import TEST.testApp.lep.service.TestLepService

return [
    createdByCallNew: new TestLepService(lepContext),
    createdByServiceFactory: lepContext.lepServices.getInstance(TestLepService)
]
