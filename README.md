Simple Browser based on the specification at https://html.spec.whatwg.org/multipage/parsing.html

How to build?
```
mvn clean install
```

Run pitest (mutation test)
```
mvn test-compile org.pitest:pitest-maven:mutationCoverage
```