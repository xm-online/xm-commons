// Check 'lepContext' binding
assert (binding.lepContext != null) : "Binding variable 'lepContext' is null"
assert (lepContext != null) : "Global variable 'lepContext' is null"

// Check 'tenantContext' binding
assert (lepContext.tenantContext != null) : "Binding variable 'tenantContext' is null"

// Check 'authContext' binding
assert (lepContext.authContext != null) : "Binding variable 'authContext' is null"

// Check 'inArgs' binding
assert (lepContext.inArgs != null) : "Binding variable 'inArgs' is null"

assert lepContext.inArgs.name == 'John Doe'
assert lepContext.inArgs.age == 23

// Check 'lep' binding (ProcessingLep is null for default script, so just check it exist)
lepContext.lep
