[![Build Status](https://travis-ci.org/bloomreach-forge/hst-pdf-renderer.svg?branch=develop)](https://travis-ci.org/bloomreach-forge/hst-pdf-renderer)

# HST PDF Renderer

**HST PDF Renderer** basically transforms HTML output into PDF output.
It leverages [Flying Saucer](http://code.google.com/p/flying-saucer/) library to generate PDF output.
Because Flying Saucer requires valid XHTML input for PDF generation,
**HST PDF Renderer** supports transformation from HTML markups to valid XHTML markups 
by leveraging [JTidy](http://jtidy.sourceforge.net) library.
HST PDF Renderer also supports a servlet filter which transforms normal HTML output to PDF output at runtime.

# Documentation 

Documentation is available at [bloomreach-forge.github.io/hst-pdf-renderer/](https://bloomreach-forge.github.io/hst-pdf-renderer/)

The documentation is generated by this command:

```bash
$ mvn clean site:site
```

The output is in the docs directory; push it and GitHub Pages will serve the site automatically. 

