version=0.1
name=urmia-run

src=target/urmia/srv-job-run

folder=target/$(name)-$(version)
file=$(name)-$(version).deb
deb=target/$(file)
key=$(shell cat ~/.bintray)

all: deb

deb: $(deb)

$(deb): $(src)
	mkdir -p $(folder)/opt/urmia
	cp -r $(src) $(folder)/opt/urmia
	sudo chown -R 0:0 $(folder)/opt
	mkdir -p $(folder)/DEBIAN
	mkdir -p $(folder)/lib/systemd/system
	cp src/main/systemd/* $(folder)/lib/systemd/system
	cp src/main/pkg/debian/* $(folder)/DEBIAN
	chmod a+x $(folder)/DEBIAN/pre*
	chmod a+x $(folder)/DEBIAN/post*
	dpkg-deb --build $(folder)
	sudo /bin/rm -rf $(folder)

clean:
	rm -f $(deb)

verify: $(deb)
	dpkg-deb -c $(deb)

scp: $(deb)
	scp $(deb) d8vm:/var/tmp

publish: $(deb)
	@#curl -X DELETE -u $(key) https://api.bintray.com/packages/amin/deb/$(name)/$(file)
	curl -# -T $(deb) -u $(key) https://api.bintray.com/content/amin/deb/$(name)/$(version)/$(file)


