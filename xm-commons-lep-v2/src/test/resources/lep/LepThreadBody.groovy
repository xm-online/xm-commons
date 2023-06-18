def authContext = lepContext.authContext
if (authContext.isFullyAuthenticated()) {
    return lepContext.tenantContext.getTenant().get().getTenantKey().getValue()
}
return null
