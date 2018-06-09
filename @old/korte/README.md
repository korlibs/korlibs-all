# Kotlin cORoutines Template Engine

![](https://raw.githubusercontent.com/korlibs/korlibs-logos/master/128/korte.png)

[All KOR libraries](https://github.com/korlibs/kor)

Port of my template engine based on twig/django [atpl.js](https://github.com/korlibs/atpl.js) + goodies.

Allow **suspension points** on read properties and method calling. Uses korio and virtual filesystems.

It intends to be a non strict superset with common parts (when finished) of of [twig](http://twig.sensiolabs.org/)/[jinja](http://jinja.pocoo.org/)/[django](https://www.djangoproject.com/)
+ [liquid](https://github.com/Shopify/liquid/wiki) except some project specific stuff.
So a kotlin [jekyll](https://jekyllrb.com/) port can happen.
Also you will be able migrate websites using twig to kotlin easier.
That jekyll-like will be able to compile into a single nodejs javascript/or native executable using [jtransc](https://github.com/jtransc/jtransc) and/or kotlin.js
so no more ruby installing issues or file watching issues on windows.

## Live demo

Online interactive real-time live demo using [Korte](https://github.com/korlibs/korte) + [Korui](https://github.com/korlibs/korui) compiled to JavaScript using [JTransc](https://github.com/jtransc/jtransc):

[https://korlibs.github.io/kor_samples/korte1/](https://korlibs.github.io/kor_samples/korte1/)

[![](docs/korte_sample.png)](https://korlibs.github.io/kor_samples/korte1/)

## Supported stuff

### Jekyll FrontMatter + layout + {{ content }}

```django
mylayout:
<side>My side</side><article>{{ content }}</article>

template:
---
layout: mylayout
---
Content
```

### Twig inheritance

```django
mylayout:
<body>
<side>My side</side><article>{{ content }}</article>
</body>

template:
---
layout: mylayout
---
Content
```

### Tags syntax

It supports normal twig/liquid blocks.
Though it supports both `{% tag %}{% endtag %}` and just `{% tag %}{% end %}` for convenience.
In examples I will just use `{% end %}` on required blocks for the sake of brevity.

Examples of already supported syntax:

```django

{# inheritance + cross templates #}
{% extends "parent" %} {# plain twig extends #}
{% extends (mobile) ? "mobile" : "desktop" %} {# conditional extends with arbitrary expressions #}
{% block name %}...{% end %} {# twig blocks inheritance #}
{% include "templatetoinclude" %} {# template to include #}

{# for #}
{% for v in [1, 2, 3, 4] %}{{ v }}{% end %} {# plain for + array notation #}
{% for k, v in {"a":1, "b": 2} %}{{ k }}{{ v }}{% end %} {# for with maps and keys + object notation #}
{% for v in collection %}{{ v }}{% else %}empty{% end %} {# for+else for empty collections #}
{% for n in [1,2,3] %}{{ n }}:{{ loop.index0 }}:{{ loop.index }}:{{ loop.revindex }}:{{ loop.revindex0 }}:{{ loop.first }}:{{ loop.last }}:{{ loop.length }}{{ '\\n' }}{% end %} {# for with loop object information about the iteration step #}

{# if #}
{% if mobile %}just for mobile{% end %}
{% if mobile %}mobile{% else %}desktop{% end %}
{% if a == 0 %}zero{% elseif a < 5 %}less than five{% else %}other{% end %}
{% if 1 in [1, 2, 3] %}collection contains one{% end %}

{# debug #}
{% debug "hello" %} {# will display by the standard output the string hello (used for debuging) #}

{# set #}
{# set name = "Test" #} {% will set a variable in this scope %}

```

### Allows extending Tags + Blocks + Filters

```kotlin
class TemplateConfig(
	extraTags: List<Tag> = listOf(),
	extraFilters: List<Filter> = listOf()
)
```

