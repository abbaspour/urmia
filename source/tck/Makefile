
all: test

../srv-obj-api/target/urmia/srv-obj-api:
	(cd ../srv-obj-api; make)

test: ../srv-obj-api/target/urmia/srv-obj-api
	bin/setup.sh
	prove -v
	bin/teardown.sh
