version=0.1.0
name=urmia-$(MODULE)

source=../../../source
src=$(source)/srv-$(MODULE)/target/urmia/srv-$(MODULE)

folder=target/$(name)-$(version)
file=$(name)-$(version).deb
deb=target/$(file)
key=$(shell cat ~/.bintray)
TEST_HOST=d8vm

.PHONY : verify

all: deb

deb: $(deb)

$(deb): $(src)
	mkdir -p $(folder)/opt/urmia
	cp -r $(src) $(folder)/opt/urmia
	mkdir -p $(folder)/DEBIAN
	mkdir -p $(folder)/lib/systemd/system
	cp systemd/* $(folder)/lib/systemd/system
	cp -r DEBIAN $(folder)
	chmod a+x $(folder)/DEBIAN/pre*
	chmod a+x $(folder)/DEBIAN/post*
	fakeroot dpkg-deb --build $(folder) $(deb)

$(src):
	cd $(source); mvn -Pdocker package; cd -

clean:
	rm -rf target

verify: $(deb)
	dpkg-deb -c $(deb)

list: verify

scp: $(deb)
	scp $(deb) $(TEST_HOST):/var/tmp/urmia-deb

publish: $(deb)
	curl -T $(deb) -u $(key) https://api.bintray.com/content/amin/deb/$(name)/$(version)/$(file)


