# Build file for mdcat

.PHONY: all clean lint check uberjar set-version graal package

version := $(shell grep defproject project.clj | cut -d ' ' -f 3 | tr -d \")
platform := $(shell uname -s | tr '[:upper:]' '[:lower:]')
uberjar_path := target/uberjar/mdcat.jar

# Graal settings
GRAAL_ROOT ?= /tmp/graal
GRAAL_VERSION ?= 21.0.0
GRAAL_HOME ?= $(GRAAL_ROOT)/graalvm-ce-java11-$(GRAAL_VERSION)
graal_archive := graalvm-ce-java11-$(platform)-amd64-$(GRAAL_VERSION).tar.gz

# Rewrite darwin as a more recognizable OS
ifeq ($(platform),darwin)
platform := macos
GRAAL_HOME := $(GRAAL_HOME)/Contents/Home
endif


all: mdcat


### Utilities ###

clean:
	rm -rf dist mdcat target

lint:
	clj-kondo --lint src test
	lein yagni

check:
	lein check

new-version=$(version)
set-version:
	@echo "Setting project and doc version to $(new-version)"
	@sed -i '' \
	    -e 's|^(defproject mdcat ".*"|(defproject mdcat "$(new-version)"|' \
	    project.clj
	@sed -i '' \
	    -e 's|mdcat ".*"|mdcat "$(new-version)"|' \
	    -e 's|MDCAT_VERSION: .*|MDCAT_VERSION: $(new-version)|' \
	    -e 's|mdcat.git", :tag ".*"}|mdcat.git", :tag "$(new-version)"}|' \
	@echo "$(new-version)" > VERSION.txt


### GraalVM Install ###

$(GRAAL_ROOT)/fetch/$(graal_archive):
	@mkdir -p $(GRAAL_ROOT)/fetch
	curl --location --output $@ https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-$(GRAAL_VERSION)/$(graal_archive)

$(GRAAL_HOME): $(GRAAL_ROOT)/fetch/$(graal_archive)
	tar -xz -C $(GRAAL_ROOT) -f $<

$(GRAAL_HOME)/bin/native-image: $(GRAAL_HOME)
	$(GRAAL_HOME)/bin/gu install native-image

graal: $(GRAAL_HOME)/bin/native-image


### Local Build ###

SRC := project.clj $(shell find resources -type f) $(shell find src -type f)

$(uberjar_path): $(SRC)
	script/uberjar

uberjar: $(uberjar_path)

mdcat: $(uberjar_path) $(GRAAL_HOME)/bin/native-image
	GRAAL_HOME=$(GRAAL_HOME) script/compile



#### Distribution Packaging ###

release_jar := mdcat-$(version).jar
release_macos_tgz := mdcat_$(version)_macos.tar.gz
release_macos_zip := mdcat_$(version)_macos.zip
release_linux_tgz := mdcat_$(version)_linux.tar.gz
release_linux_zip := mdcat_$(version)_linux.zip
release_linux_static_zip := mdcat_$(version)_linux_static.zip

# Uberjar
dist/$(release_jar): $(uberjar_path)
	@mkdir -p dist
	cp $< $@

# Mac OS X
ifeq ($(platform),macos)
dist/$(release_macos_tgz): mdcat
	@mkdir -p dist
	tar -cvzf $@ $^

dist/$(release_macos_zip): mdcat
	@mkdir -p dist
	zip $@ $^
endif

# Linux
target/package/linux/mdcat: Dockerfile $(SRC)
	script/docker-build --output $@

dist/$(release_linux_tgz): target/package/linux/mdcat
	@mkdir -p dist
	tar -cvzf $@ -C $(dir $<) $(notdir $<)

dist/$(release_linux_zip): target/package/linux/mdcat
	@mkdir -p dist
	cd $(dir $<); zip $(abspath $@) $(notdir $<)

# Linux (static)
target/package/linux-static/mdcat: Dockerfile $(SRC)
	script/docker-build --static --output $@

dist/$(release_linux_static_zip): target/package/linux-static/mdcat
	@mkdir -p dist
	cd $(dir $<); zip $(abspath $@) $(notdir $<)

# Metapackage
ifeq ($(platform),macos)
package: dist/$(release_jar) dist/$(release_macos_tgz) dist/$(release_macos_zip) dist/$(release_linux_tgz) dist/$(release_linux_zip) dist/$(release_linux_static_zip)
else
package: dist/$(release_jar) dist/$(release_linux_tgz) dist/$(release_linux_zip) dist/$(release_linux_static_zip)
endif
