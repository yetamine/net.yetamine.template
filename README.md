# net.yetamine.template #

This repository contains a library for supporting string templates with resolvable placeholders.

The library defines a set of rich and fluent interfaces for implementing various template types even with different syntax rules. These interfaces decouple clients, that want use a template, from all the details. Placeholders in the templates can be resolved with a custom resolving function, usually provided by a client.

The library bundles support for templates with placeholders surrounded with user-defined brackets (with the possibility to define an escape sequence). Although the bundled parser does not support nested placeholders (i.e., placeholders containing whole templates with their own placeholders), the library provides a recursive resolver that allows a placeholder to resolve to a nested template. The recursive resolver provides a lof of hooks to handle cycles, failures of nested template parsing, failures of template lookups, context-sensitive placeholders etc. The resolver can cache the results as well while it remains thread-safe.

Both bundled parts cover probably most needs, but if it is not sufficient, it could be enough to supply a smarter parser that handles different template syntax (perhaps supporting nested placeholders) and combine such a smarter parser with the bundled recursive resolver to get really powerful recursion-capable templates.


## Examples ##

```java
// Let's have a Map with some values:
final Map<String, String> map = new HashMap<>();
map.put("name", "Kitty");
map.put("color", "pink");

// Let's get the template format, the standard uses ${placeholder} convention
final TemplateFormat format = Interpolation.standard();
// This prints "Hello Kitty! Do you like pink color? And what about ${meal}?"
// The last placeholder is kept, because there is no such mapping in our map for "meal".
// The resolving function (our map::get) returns null then, which means "keep the placeholder".
System.out.println(format.resolve("Hello ${name}! Do you like ${color} color? And what about ${meal}?", map::get));

// Escaping a placeholder with the standard format is simple as well.
// And we can get the escaped value for any string easily too:
System.out.println(format.constant("Hello ${name}!"));
// This prints "Hello $${name}!", which is the escaped form of the argument and remains constant:
System.out.println(format.resolve("Hello $${name}!", map::get)); // Just "Hello ${name}!"

// And if you don't like the default brackets, make your own format:
System.out.println(Interpolation.with("$[", "]", "\\").resolve("Hello $[name]! No \\$[color]!", map::get));

// You can get a pre-parsed templates, template resolving functions and more! 
```

And here a teaser for recursive template resolution:

```java
// Let's have a Map with some templates, perhaps from a .properties file
final Map<String, String> map = new HashMap<>();
map.put("host", "localhost");
map.put("port", "443");
map.put("path", "/index.html");
map.put("protocol", "https");
map.put("url", "${protocol}://${host}:${port}${path}");

// Let's make a resolver with the default settings reading from the map
final UnaryOperator<String> resolver = TemplateRecursion.with(map::get).build();

// We can use it directly to get "https://localhost:443/index.html"
System.out.println(resolver.apply("url"));

// Or we can plug it into a usual template resolution
final TemplateFormat format = Interpolation.standard();
System.out.println(format.resolve("Could not connect to ${url}.", resolver));
```


## Prerequisites ##

For building this project is needed:

* JDK 8 or newer.
* Maven 3.6 or newer.

For using the library is needed:

* JRE 8 or newer.


## Licensing ##

The project is licensed under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0). Contributions to the project are welcome and accepted if they can be incorporated without the need of changing the license or license conditions and terms.


[![Yetamine logo](https://github.com/pdolezal/net.yetamine/raw/master/about/Yetamine_small.png "Our logo")](https://github.com/pdolezal/net.yetamine/blob/master/about/Yetamine_large.png)
