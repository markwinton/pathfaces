# Pathfaces

Java Server Faces (JSF) library to allow pretty URLs in JSF applications.

## Usage

1. Add the dependency to your project in the usual manner.
2. Add a `rewrite-url.xml` to the `META-INF` directory - see [rewrite-url.xml](#rewrite-urlxml) for details.
3. Add the `Pathfaces` rewrite filter to your `web.xml` - see [faces-config.xml](#faces-configxml) for details.

## rewrite-url.xml

Below is an example entry for `rewrite-url.xml`:

```xml
<url-mapping id="home">
    <pattern value="/home/#{title}"/>
    <view-id value="/index.xhtml"/>
</url-mapping>
```

With this mapping in place the URL `/home/hello-world` will be rewritten to `/index.xhtml?title=hello-world`.

## faces-config.xml

Below is an example entry for `faces-config.xml`:

```xml
<?xml version='1.0' encoding='UTF-8'?>
<faces-config version="2.2"
              xmlns="http://xmlns.jcp.org/xml/ns/javaee"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facesconfig_2_2.xsd">
    <application>
        <view-handler>dev.markwinton.pathfaces.RewriteURLFilter</view-handler>
    </application>
</faces-config>
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE.txt) file for details.