#!/bin/bash
set -e 

VERSION=`grep version build.gradle | head -1 | tr -d "'" | awk '{print $NF}'`

gradle clean javadoc

(
cd www 
middleman build
rm -rf build/javadoc/${VERSION}
cp -r ../build/docs/javadoc build/javadoc/${VERSION}
rm -rf build/javadoc/latest
cp -r ../build/docs/javadoc build/javadoc/latest
rm -rf source/javadoc/${VERSION}
cp -r ../build/docs/javadoc source/javadoc/${VERSION}
)

git add . && git commit -am "Add javadocs for ${VERSION}"

rm -rf /tmp/oio-java-client-docs-${VERSION}

cp -r www/build /tmp/oio-java-client-docs-${VERSION}

git checkout gh-pages
git clean -fd
cp -r /tmp/oio-java-client-docs-${VERSION}/* .

git add . && git commit -am "Add javadocs for ${VERSION}"
git push origin gh-pages:gh-pages

git checkout master

