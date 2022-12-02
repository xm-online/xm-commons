## xm-commons-migration-db (independent)
XM^online commons project for database migration help classes.

### `@Jsonb` annotation
To be able to work with jsonb data type on postgresql database, you need to put `@Jsonb` annotation on 
entity field that will be saved as json.<br/>
Example:
```
@Entity
@Table(name = "data")
@Getter
@Setter
public class Data {

    @Jsonb
    @Column(name = "payload")
    private String payload;
    
    @Column(name = "not_jsonb_column")
    private String notJsonbField;
}
```
To enable jsonb annotation processor add file to your resources folder:<br/>
file path:<br/>
`META-INF/services/org.hibernate.boot.spi.SessionFactoryBuilderFactory`<br/>
file content:<br/>
`com.icthh.xm.commons.migration.db.jsonb.JsonbTypeRegistrator`
