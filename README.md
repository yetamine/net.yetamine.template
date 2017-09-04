# net.yetamine.template #

This repository contains a library for supporting simple string templates.

The templates can contain placeholders that can be resolved using a custom resolving function. A placeholder occurrence appears enclosed between a custom-defined opening and closing sequences and the opening sequence, which triggers recognition of a placeholder, can be escaped with yet another custom-defined sequence. The implementation supports parsing and formatting of the templates and provides a rich set of generic interfaces that can be used for implementing a very different types of templates.

The bundled implementation does not support recursive templates (i.e., a placeholder can't contain another placeholder), however, it does not exclude the possibility of recursive templates, because this capability depends on the resolving function only. Moreover, a more advanced implementation, which would use the same interfaces, can be provided to support even more complex or nested placeholders.


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


## Prerequisites ##

For building this project is needed:

* JDK 8 or newer.
* Maven 3.3 or newer.

For using the built library is needed:

* JRE 8 or newer.


## Licensing ##

The project is licensed under the [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0). Contributions to the project are welcome and accepted if they can be incorporated without the need of changing the license or license conditions and terms.


[![Yetamine logo](https://github.com/pdolezal/net.yetamine/raw/master/about/Yetamine_small.png "Our logo")](https://github.com/pdolezal/net.yetamine/blob/master/about/Yetamine_large.png)
