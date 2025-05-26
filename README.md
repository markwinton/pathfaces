# Pathfaces

Jakarta Server Faces (JSF) library to allow pretty URLs in JSF applications.

## Usage

1. Add the dependency to your project in the usual manner.
2. Add a `rewrite-url.xml` to the `META-INF` directory - see [rewrite-url.xml](#rewrite-urlxml) for details.
3. Add the `Pathfaces` rewrite filter to your `web.xml` - see [faces-config.xml](#faces-configxml) for details.
4. Register the filter in your application

## rewrite-url.xml

Below are example entries for `rewrite-url.xml`.

### Rewrite URL

With the following mapping in place the URL `/home/hello-world` will be rewritten to `/index.xhtml?title=hello-world`.

```xml
<url-mapping id="home">
    <pattern value="/home/#{title}"/>
    <view-id value="/index.xhtml"/>
</url-mapping>
```

### Ignore URL

With the following mapping in place any calls to `/api/` and `/api/*` will be omitted from any other path matching:

```xml
<ignored-path>
    <path value="/api/"/>
    <is-exact value="false"/>
</ignored-path>
```

With the following mapping in place any calls to `/api/` will be omitted from any other path matching, but only if
the path is an exact match - for example `/api/` will be ignored, but `/api/some-action` will not.

```xml
<ignored-path>
    <path value="/api/"/>
    <is-exact value="true"/>
</ignored-path>
```

## faces-config.xml

Below is an example entry for `faces-config.xml`:

```xml
<?xml version='1.0' encoding='UTF-8'?>
<faces-config version="2.2"
              xmlns="http://xmlns.jcp.org/xml/ns/javaee"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facesconfig_2_2.xsd">
    <application>
        <view-handler>io.github.markwinton.pathfaces.RewriteViewHandler</view-handler>
    </application>
</faces-config>
```

## Registering the Filter

An example class which needs to be added to your project to register the filter is shown below.
This class should be in the `src/main/java` directory of your project.

```java
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import io.github.markwinton.pathfaces.RewriteURLFilter;

@WebListener
public class FilterRegistrationListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        final ServletContext context = sce.getServletContext();
        final FilterRegistration.Dynamic filter = context.addFilter("RewriteURLFilter", RewriteURLFilter.class);
        filter.addMappingForUrlPatterns(null, true, "/*");
    }
}
```


## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE.txt) file for details.