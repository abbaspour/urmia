# http://www.perkin.org.uk/posts/creating-local-smartos-packages.html

# http://netbsd.gw.com/cgi-bin/man-cgi?pkg_create+1+NetBSD-5.0
# http://www.perkin.org.uk/posts/creating-local-smartos-packages.html

version := 0.1

package := target/urmia-mds-$(version).tgz
src := ../../../srv-obj-api/target/urmia/srv-obj-api

## plist files
plist := target/plist
plist-head := target/plist-head
plist-lib := target/plist-lib
plist-bin := target/plist-bin
plist-etc := target/plist-etc
plist-svc := target/plist-svc

## pkgsrc
pkgsrc := pkgsrc

all: $(package)

$(src):
	@(cd ..; mvn -am -pl srv-obj-api package; cd -)

$(plist-head):
	mkdir -p target
	@echo "@name urmia-mds-0.1" > $(plist-head)
	@echo "@owner urmia" >> $(plist-head)
	@echo "@group urmia" >> $(plist-head)
	@echo "@pkgdep openjdk7" >> $(plist-head)
	@echo "@cwd /opt" >> $(plist-head)

$(plist-lib): $(src)
	mkdir -p target
	@echo "@mode 644" > $(plist-lib)
	@(cd $(src); find urmia/srv-obj-api/lib -type f ) >> $(plist-lib)

$(plist-bin): $(src)
	@echo "@mode 755" > $(plist-bin)
	@(cd $(src); find urmia/srv-obj-api/bin -type f -name \*.sh ) >> $(plist-bin)

$(plist-etc): $(src)
	@echo "@mode 655" > $(plist-etc)
	@(cd $(src); find urmia/srv-obj-api/etc -type f ) >> $(plist-etc)

$(plist-svc): $(src)
	@echo "@mode 655" > $(plist-svc)
	@(cd .; find smf -type f ) >> $(plist-svc)

$(plist): $(plist-lib) $(plist-bin) $(plist-etc) $(plist-svc) $(plist-head)
	@cat $(plist-head) $(plist-bin) $(plist-lib) $(plist-etc) $(plist-svc) > $(plist)

$(package): $(plist)
	/usr/pkg/sbin/pkg_create -B ${pkgsrc}/build-info -c ${pkgsrc}/comment -d ${pkgsrc}/description \
	    -i ${pkgsrc}/iscript.sh -k ${pkgsrc}/dscript.sh -g urmia -u urmia -D ${pkgsrc}/displayfile \
	    -f $(plist) -I /opt -p ../../../srv-obj-api/target -U $(package)

info: $(package)
	@/usr/pkg/sbin/pkg_info -X $(package)

list: $(package)
	@/usr/pkg/sbin/pkg_info -L $(package)

target/pkg_summary.gz: $(package)
	@/usr/pkg/sbin/pkg_info -X $(package) | gzip -9 > target/pkg_summary.gz

scp: $(package) target/pkg_summary.gz
	@scp $(package) target/pkg_summary.gz um01:~/pkg

clean:
	@rm -f $(package) target/plist-* $(plist) target/pkg_summary.gz

mvnclean:
	@mvn clean