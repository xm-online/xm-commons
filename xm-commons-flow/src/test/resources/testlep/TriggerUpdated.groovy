import com.icthh.xm.commons.config.domain.Configuration

List<Configuration> configurations = new ArrayList<Configuration>()
configurations.add(new Configuration("/config/tenants/TEST/test/somespec.yml", "blablabla"));
configurations.add(new Configuration("/config/tenants/TEST/test/otherspec.yml", "foofoofoo"));

return configurations
